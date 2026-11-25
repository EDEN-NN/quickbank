package com.quickbank.ledger.app;

import com.quickbank.common.event.payload.AccountOpenedPayload;
import com.quickbank.common.event.payload.PaymentCompletedPayload;
import com.quickbank.common.money.AccountNumbers;
import com.quickbank.common.security.ClientUser;
import com.quickbank.common.trace.QuickBankSpans;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class LedgerApplicationService {

    private final LedgerWorkflow workflow;
    private final QuickBankSpans spans;
    private final TransactionTemplate transaction;

    public LedgerApplicationService(LedgerWorkflow workflow, QuickBankSpans spans, PlatformTransactionManager transactions) {
        this.workflow = workflow;
        this.spans = spans;
        this.transaction = new TransactionTemplate(transactions);
    }

    public DepositReceipt acceptDeposit(ClientUser user, String idempotencyKey, BigDecimal amount) {
        try {
            return transaction.execute(status -> observeDeposit(user, idempotencyKey, amount));
        } catch (DataIntegrityViolationException ex) {
            return transaction.execute(status -> observeDeposit(user, idempotencyKey, amount));
        }
    }

    public Optional<DepositReceipt> findDeposit(String depositId, ClientUser user) {
        TransactionTemplate read = new TransactionTemplate(transaction.getTransactionManager());
        read.setReadOnly(true);
        return read.execute(status -> workflow.findDeposit(depositId, user.subject()));
    }

    public void postPayment(PaymentCompletedPayload result) {
        transaction.executeWithoutResult(status -> spans.run(
                "quickbank.ledger.deposit.post",
                Map.of("module", "ledger", "mock", "false", "movement.type", "DEPOSIT"),
                Map.of("account.number", result.accountNumber() == null ? "unknown" : result.accountNumber()),
                () -> workflow.postPayment(result)));
    }

    public TransferReceipt transfer(ClientUser user, String idempotencyKey, BigDecimal amount, String toAccount) {
        String destination = AccountNumbers.require(toAccount);
        try {
            return transaction.execute(status -> observeTransfer(user, idempotencyKey, amount, destination));
        } catch (DataIntegrityViolationException ex) {
            return transaction.execute(status -> observeTransfer(user, idempotencyKey, amount, destination));
        }
    }

    public void provision(AccountOpenedPayload payload) {
        try {
            transaction.executeWithoutResult(status -> workflow.provision(payload));
        } catch (DataIntegrityViolationException ignored) {
            // A redelivered AccountOpened loses the race to the unique account number.
        }
    }

    private DepositReceipt observeDeposit(ClientUser user, String idempotencyKey, BigDecimal amount) {
        return spans.run(
                "quickbank.ledger.deposit.accept",
                Map.of("module", "ledger", "mock", "false", "movement.type", "DEPOSIT"),
                Map.of(),
                bag -> {
                    DepositReceipt receipt = workflow.acceptDeposit(user, idempotencyKey, amount);
                    bag.attribute("account.number", receipt.accountNumber());
                    return receipt;
                });
    }

    private TransferReceipt observeTransfer(ClientUser user, String idempotencyKey, BigDecimal amount, String destination) {
        return spans.run(
                "quickbank.ledger.transfer",
                Map.of("module", "ledger", "mock", "false", "movement.type", "TRANSFER"),
                Map.of("account.number", destination),
                bag -> {
                    TransferReceipt receipt = workflow.transfer(user, idempotencyKey, amount, destination);
                    bag.attribute("account.number", receipt.fromAccount());
                    bag.attribute("transfer.id", receipt.transferId());
                    return receipt;
                });
    }
}
