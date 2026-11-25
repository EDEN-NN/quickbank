package com.quickbank.ledger.app;

import com.quickbank.common.error.AccountNotProvisionedException;
import com.quickbank.common.error.IdempotencyConflictException;
import com.quickbank.common.error.UnknownAccountException;
import com.quickbank.common.error.SelfTransferException;
import com.quickbank.common.event.EventFactory;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.quickbank.common.event.payload.AccountOpenedPayload;
import com.quickbank.common.event.payload.DepositPostedPayload;
import com.quickbank.common.event.payload.PaymentCompletedPayload;
import com.quickbank.common.event.payload.PaymentRequestedPayload;
import com.quickbank.common.event.payload.TransferPostedPayload;
import com.quickbank.common.money.AccountNumbers;
import com.quickbank.common.money.Money;
import com.quickbank.common.security.ClientUser;
import com.quickbank.ledger.domain.JournalPlan;
import com.quickbank.ledger.domain.LedgerPostingPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LedgerWorkflow {

    private final LedgerStore store;
    private final EventFactory events;
    private final Clock clock;

    public LedgerWorkflow(LedgerStore store, EventFactory events, Clock clock) {
        this.store = store;
        this.events = events;
        this.clock = clock;
    }

    public void provision(AccountOpenedPayload payload) {
        if (store.findByNumber(payload.accountNumber()).isPresent()) {
            return;
        }
        store.insertCustomer(new CustomerBook(
                payload.accountNumber(),
                payload.ownerSubject(),
                payload.ownerEmail(),
                payload.ownerName(),
                Money.ZERO,
                "CUSTOMER"));
    }

    public DepositReceipt acceptDeposit(ClientUser user, String idempotencyKey, BigDecimal amount) {
        BigDecimal normalized = Money.amount(amount);
        Optional<StoredDeposit> existing = store.findDepositByKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().matches(user.subject(), normalized)) {
                throw new IdempotencyConflictException();
            }
            return receipt(existing.get());
        }
        CustomerBook customer = store.findByOwner(user.subject()).orElseThrow(AccountNotProvisionedException::new);
        Instant now = clock.instant();
        StoredDeposit deposit = new StoredDeposit(
                UUID.randomUUID().toString(),
                idempotencyKey,
                user.subject(),
                customer.accountNumber(),
                normalized,
                "PENDING",
                now,
                null);
        store.insertDeposit(deposit);
        store.enqueue(
                Topics.PAYMENT_COMMANDS,
                customer.accountNumber(),
                events.create(
                        EventTypes.PAYMENT_REQUESTED,
                        idempotencyKey,
                        new PaymentRequestedPayload(
                                deposit.id(),
                                customer.accountNumber(),
                                Money.format(normalized),
                                Money.CURRENCY,
                                idempotencyKey,
                                "DEPOSIT")));
        return receipt(deposit);
    }

    public Optional<DepositReceipt> findDeposit(String depositId, String ownerSubject) {
        return store.findDepositById(depositId)
                .filter(deposit -> deposit.ownerSubject().equals(ownerSubject))
                .map(this::receipt);
    }

    public void postPayment(PaymentCompletedPayload result) {
        if (!"SUCCEEDED".equals(result.status())) {
            throw new IllegalStateException("Payment mock must succeed. Got " + result.status());
        }
        StoredDeposit deposit = store.lockDeposit(result.paymentId()).orElse(null);
        if (deposit == null || deposit.posted()) {
            return;
        }
        List<String> order = new ArrayList<>(List.of(deposit.accountNumber(), LedgerPostingPolicy.VAULT));
        order.sort(Comparator.naturalOrder());
        CustomerBook customer = null;
        CustomerBook vault = null;
        for (String number : order) {
            if (LedgerPostingPolicy.VAULT.equals(number)) {
                vault = store.lockVault();
            } else {
                customer = store.lockCustomer(number).orElseThrow(() -> new UnknownAccountException(number));
            }
        }
        JournalPlan plan = LedgerPostingPolicy.deposit(
                customer.accountNumber(),
                deposit.amount(),
                customer.cashBalance(),
                vault.cashBalance());
        requireBalanced(plan);
        Instant now = clock.instant();
        String journalId = UUID.randomUUID().toString();
        store.insertJournal(journalId, "DEPOSIT", deposit.idempotencyKey(), plan, now);
        BigDecimal running = plan.balancesAfter().get(customer.accountNumber());
        store.saveBalance(customer.withBalance(running));
        store.saveBalance(vault.withBalance(plan.balancesAfter().get(LedgerPostingPolicy.VAULT)));
        store.markDepositPosted(deposit.id(), now);
        store.enqueue(
                Topics.LEDGER_EVENTS,
                customer.accountNumber(),
                events.create(
                        EventTypes.DEPOSIT_POSTED,
                        deposit.idempotencyKey(),
                        new DepositPostedPayload(
                                deposit.id(),
                                customer.accountNumber(),
                                Money.format(deposit.amount()),
                                Money.CURRENCY,
                                Money.format(running),
                                now)));
    }

    public TransferReceipt transfer(ClientUser user, String idempotencyKey, BigDecimal amount, String toAccountRaw) {
        String destination = AccountNumbers.require(toAccountRaw);
        BigDecimal normalized = Money.amount(amount);
        Optional<StoredTransfer> existing = store.findTransferByKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().matches(user.subject(), destination, normalized)) {
                throw new IdempotencyConflictException();
            }
            return receipt(existing.get());
        }
        CustomerBook source = store.findByOwner(user.subject()).orElseThrow(AccountNotProvisionedException::new);
        if (source.accountNumber().equals(destination)) {
            throw new SelfTransferException(source.accountNumber());
        }
        List<String> order = new ArrayList<>(List.of(source.accountNumber(), destination));
        order.sort(Comparator.naturalOrder());
        CustomerBook lockedSource = null;
        CustomerBook lockedDestination = null;
        for (String number : order) {
            Optional<CustomerBook> locked = store.lockCustomer(number);
            if (number.equals(source.accountNumber())) {
                lockedSource = locked.orElseThrow(AccountNotProvisionedException::new);
            } else if (locked.isPresent() && locked.get().customer()) {
                lockedDestination = locked.get();
            }
        }
        if (lockedDestination == null) {
            throw new UnknownAccountException(destination);
        }
        JournalPlan plan = LedgerPostingPolicy.transfer(
                lockedSource.accountNumber(),
                lockedDestination.accountNumber(),
                normalized,
                lockedSource.cashBalance(),
                lockedDestination.cashBalance());
        requireBalanced(plan);
        Instant now = clock.instant();
        String transferId = UUID.randomUUID().toString();
        store.insertJournal(transferId, "TRANSFER", idempotencyKey, plan, now);
        BigDecimal fromAfter = plan.balancesAfter().get(lockedSource.accountNumber());
        BigDecimal toAfter = plan.balancesAfter().get(lockedDestination.accountNumber());
        store.saveBalance(lockedSource.withBalance(fromAfter));
        store.saveBalance(lockedDestination.withBalance(toAfter));
        StoredTransfer transfer = new StoredTransfer(
                transferId,
                idempotencyKey,
                user.subject(),
                lockedSource.accountNumber(),
                lockedDestination.accountNumber(),
                normalized,
                fromAfter,
                toAfter,
                now);
        store.insertTransfer(transfer);
        store.enqueue(
                Topics.LEDGER_EVENTS,
                lockedSource.accountNumber(),
                events.create(
                        EventTypes.TRANSFER_POSTED,
                        idempotencyKey,
                        new TransferPostedPayload(
                                transferId,
                                lockedSource.accountNumber(),
                                lockedDestination.accountNumber(),
                                Money.format(normalized),
                                Money.CURRENCY,
                                Money.format(fromAfter),
                                Money.format(toAfter),
                                now)));
        return receipt(transfer);
    }

    private static void requireBalanced(JournalPlan plan) {
        if (!plan.balanced()) {
            throw new IllegalStateException("Refusing to commit an unbalanced journal.");
        }
    }

    private DepositReceipt receipt(StoredDeposit deposit) {
        return new DepositReceipt(
                deposit.id(),
                deposit.status(),
                deposit.accountNumber(),
                Money.format(deposit.amount()),
                Money.CURRENCY);
    }

    private TransferReceipt receipt(StoredTransfer transfer) {
        return new TransferReceipt(
                transfer.id(),
                transfer.fromAccount(),
                transfer.toAccount(),
                Money.format(transfer.amount()),
                Money.format(transfer.fromBalanceAfter()),
                Money.format(transfer.toBalanceAfter()),
                Money.CURRENCY);
    }
}
