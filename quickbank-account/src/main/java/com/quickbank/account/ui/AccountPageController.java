package com.quickbank.account.ui;

import com.quickbank.account.app.AccountApplicationService;
import com.quickbank.account.app.AccountApplicationService.AccountView;
import com.quickbank.account.app.StatementLine;
import com.quickbank.common.error.QuickBankException;
import com.quickbank.common.money.Money;
import com.quickbank.common.security.ClientUsers;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Controller
public class AccountPageController {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(SAO_PAULO);

    private final AccountApplicationService accounts;

    public AccountPageController(AccountApplicationService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/account")
    public String account(Model model) {
        AccountView view = accounts.findMine(ClientUsers.require()).orElse(null);
        model.addAttribute("account", view);
        model.addAttribute("rows", view == null ? List.of() : rows(accounts.statement(ClientUsers.require())).stream().limit(5).toList());
        model.addAttribute("depositKey", UUID.randomUUID().toString());
        model.addAttribute("transferKey", UUID.randomUUID().toString());
        model.addAttribute("zone", "America/Sao_Paulo");
        if (view != null) {
            model.addAttribute("cash", Money.display(view.cashAvailable()));
            model.addAttribute("creditLimit", Money.display(view.creditLimit()));
            model.addAttribute("creditUsed", Money.display(view.creditUsed()));
            model.addAttribute("creditAvailable", Money.display(view.creditAvailable()));
        }
        return "account";
    }

    @PostMapping("/account")
    public String open(RedirectAttributes redirect) {
        try {
            AccountView opened = accounts.open(ClientUsers.require());
            redirect.addFlashAttribute("notice",
                    "Account " + opened.accountNumber() + " is open. Cash starts at "
                            + Money.display(opened.cashAvailable())
                            + " and the credit line is " + Money.display(opened.creditLimit())
                            + ". A mock email was queued.");
            return "redirect:/account";
        } catch (QuickBankException ex) {
            redirect.addFlashAttribute("errorTitle", ex.title());
            redirect.addFlashAttribute("errorDetail", ex.getMessage());
            return "redirect:/account";
        }
    }

    @GetMapping("/account/statement")
    public String statement(Model model) {
        AccountView view = accounts.findMine(ClientUsers.require()).orElse(null);
        if (view == null) {
            return "redirect:/account";
        }
        List<Row> rows = rows(accounts.statement(ClientUsers.require()));
        model.addAttribute("account", view);
        model.addAttribute("rows", rows);
        model.addAttribute("zone", "America/Sao_Paulo");
        return "statement";
    }

    private List<Row> rows(List<StatementLine> lines) {
        return lines.stream()
                .sorted(StatementLine.CHRONOLOGICAL)
                .map(line -> new Row(
                        WHEN.format(line.occurredAt()),
                        label(line.type()),
                        signed(line),
                        line.counterparty(),
                        Money.display(line.runningBalance()),
                        "TRANSFER_OUT".equals(line.type())))
                .toList();
    }

    private static String label(String type) {
        return switch (type) {
            case "DEPOSIT" -> "Deposit";
            case "TRANSFER_OUT" -> "Transfer sent";
            case "TRANSFER_IN" -> "Transfer received";
            default -> type;
        };
    }

    private static String signed(StatementLine line) {
        String amount = Money.display(line.amount());
        return "TRANSFER_OUT".equals(line.type()) ? "− " + amount : "+ " + amount;
    }

    public record Row(String when, String type, String amount, String counterparty, String runningBalance, boolean outgoing) {
    }
}
