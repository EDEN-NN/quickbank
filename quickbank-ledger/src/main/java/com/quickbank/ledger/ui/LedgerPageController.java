package com.quickbank.ledger.ui;

import com.quickbank.common.error.QuickBankException;
import com.quickbank.common.money.IdempotencyKeys;
import com.quickbank.common.money.Money;
import com.quickbank.common.security.ClientUsers;
import com.quickbank.ledger.app.DepositReceipt;
import com.quickbank.ledger.app.LedgerApplicationService;
import com.quickbank.ledger.app.TransferReceipt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LedgerPageController {

    private final LedgerApplicationService ledger;

    public LedgerPageController(LedgerApplicationService ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/deposits")
    public String deposit(
            @RequestParam String amount,
            @RequestParam String idempotencyKey,
            RedirectAttributes redirect
    ) {
        try {
            DepositReceipt receipt = ledger.acceptDeposit(
                    ClientUsers.require(),
                    IdempotencyKeys.require(idempotencyKey),
                    Money.parse(amount));
            return "redirect:/deposits/" + receipt.depositId();
        } catch (QuickBankException ex) {
            redirect.addFlashAttribute("errorTitle", ex.title());
            redirect.addFlashAttribute("errorDetail", ex.getMessage());
            redirect.addFlashAttribute("depositAmount", amount);
            return "redirect:/account";
        }
    }

    @GetMapping("/deposits/{id}")
    public String depositStatus(@PathVariable String id, Model model) {
        DepositReceipt receipt = ledger.findDeposit(id, ClientUsers.require()).orElse(null);
        if (receipt == null) {
            model.addAttribute("missing", true);
            return "deposit-status";
        }
        model.addAttribute("deposit", receipt);
        model.addAttribute("pending", "PENDING".equals(receipt.status()));
        return "deposit-status";
    }

    @PostMapping("/transfers")
    public String transfer(
            @RequestParam String toAccountNumber,
            @RequestParam String amount,
            @RequestParam String idempotencyKey,
            RedirectAttributes redirect
    ) {
        try {
            TransferReceipt receipt = ledger.transfer(
                    ClientUsers.require(),
                    IdempotencyKeys.require(idempotencyKey),
                    Money.parse(amount),
                    toAccountNumber);
            redirect.addFlashAttribute("notice",
                    "Sent " + Money.display(new java.math.BigDecimal(receipt.amount()))
                            + " to " + receipt.toAccount()
                            + ". Cash left on this account: " + Money.display(new java.math.BigDecimal(receipt.fromBalanceAfter()))
                            + ". The statement catches up as soon as the ledger event is applied.");
            return "redirect:/account?watch=1";
        } catch (QuickBankException ex) {
            redirect.addFlashAttribute("errorTitle", ex.title());
            redirect.addFlashAttribute("errorDetail", ex.getMessage());
            redirect.addFlashAttribute("transferAmount", amount);
            redirect.addFlashAttribute("transferTo", toAccountNumber);
            return "redirect:/account";
        }
    }
}
