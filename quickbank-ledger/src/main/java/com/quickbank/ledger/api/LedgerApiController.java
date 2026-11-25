package com.quickbank.ledger.api;

import com.quickbank.common.money.IdempotencyKeys;
import com.quickbank.common.security.ClientUsers;
import com.quickbank.ledger.app.DepositReceipt;
import com.quickbank.ledger.app.LedgerApplicationService;
import com.quickbank.ledger.app.TransferReceipt;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class LedgerApiController {

    private static final String IDEMPOTENCY_KEY = "8 to 80 characters: letters, digits, and . _ : - . "
            + "The same key with the same amount replays the first result. A different amount or account is rejected.";

    private final LedgerApplicationService ledger;

    public LedgerApiController(LedgerApplicationService ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/accounts/me/deposits")
    public ResponseEntity<DepositReceipt> deposit(
            @RequestHeader("Idempotency-Key")
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = IDEMPOTENCY_KEY, required = true,
                    schema = @Schema(type = "string", minLength = 8, maxLength = 80, pattern = "[A-Za-z0-9._:-]{8,80}"))
            String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        DepositReceipt receipt = ledger.acceptDeposit(ClientUsers.require(), IdempotencyKeys.require(idempotencyKey), request.amount());
        HttpStatus status = "POSTED".equals(receipt.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(receipt);
    }

    @GetMapping("/deposits/{id}")
    public DepositReceipt deposit(@PathVariable String id) {
        return ledger.findDeposit(id, ClientUsers.require())
                .orElseThrow(() -> new com.quickbank.common.error.NotFoundException("No deposit with id " + id + " for this user."));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferReceipt> transfer(
            @RequestHeader("Idempotency-Key")
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = IDEMPOTENCY_KEY, required = true,
                    schema = @Schema(type = "string", minLength = 8, maxLength = 80, pattern = "[A-Za-z0-9._:-]{8,80}"))
            String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferReceipt receipt = ledger.transfer(
                ClientUsers.require(),
                IdempotencyKeys.require(idempotencyKey),
                request.amount(),
                request.toAccountNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    public record DepositRequest(
            @NotNull
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1000000.00")
            @Digits(integer = 12, fraction = 2, message = "Use at most two decimal places")
            BigDecimal amount
    ) {
    }

    public record TransferRequest(
            @NotBlank String toAccountNumber,
            @NotNull
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1000000.00")
            @Digits(integer = 12, fraction = 2, message = "Use at most two decimal places")
            BigDecimal amount
    ) {
    }
}
