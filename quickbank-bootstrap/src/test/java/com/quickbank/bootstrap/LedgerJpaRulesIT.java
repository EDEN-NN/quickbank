package com.quickbank.bootstrap;

import com.quickbank.common.CommonConfig;
import com.quickbank.common.event.payload.AccountOpenedPayload;
import com.quickbank.common.money.Money;
import com.quickbank.common.outbox.OutboxWriter;
import com.quickbank.common.security.ClientUser;
import com.quickbank.ledger.app.LedgerWorkflow;
import com.quickbank.ledger.app.TransferReceipt;
import com.quickbank.ledger.persistence.JpaLedgerStore;
import com.quickbank.ledger.persistence.LedgerAccountEntity;
import com.quickbank.ledger.persistence.LedgerAccountRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs the ledger rules through MySQL when Docker is available.
 * Surefire does not execute this class; {@code mvn verify -Pdocker-tests} does.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LedgerJpaRulesIT.LedgerSlice.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Testcontainers
@Tag("docker")
class LedgerJpaRulesIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    LedgerWorkflow workflow;

    @Autowired
    LedgerAccountRepository accounts;

    @Test
    void postsADepositOnceAndRejectsInsufficientFundsAndSelfTransfer() {
        workflow.provision(opened("10000001", "sub-ana", "ana@quickbank.local", "ana"));
        workflow.provision(opened("10000002", "sub-bruno", "bruno@quickbank.local", "bruno"));
        var ana = new ClientUser("sub-ana", "ana@quickbank.local", "ana");
        var pending = workflow.acceptDeposit(ana, "it-deposit-1", new BigDecimal("80.00"));
        workflow.postPayment(new com.quickbank.common.event.payload.PaymentCompletedPayload(
                pending.depositId(), "SUCCEEDED", "10000001", "80.00", Money.CURRENCY, "it-deposit-1"));
        workflow.postPayment(new com.quickbank.common.event.payload.PaymentCompletedPayload(
                pending.depositId(), "SUCCEEDED", "10000001", "80.00", Money.CURRENCY, "it-deposit-1"));

        assertEquals(new BigDecimal("80.00"), accounts.findById("10000001").orElseThrow().getCashBalance());
        assertThrows(com.quickbank.common.error.InsufficientFundsException.class,
                () -> workflow.transfer(ana, "it-transfer-too-much", new BigDecimal("80.01"), "10000002"));
        assertThrows(com.quickbank.common.error.SelfTransferException.class,
                () -> workflow.transfer(ana, "it-transfer-self", new BigDecimal("1.00"), "10000001"));
        assertEquals(new BigDecimal("80.00"), accounts.findById("10000001").orElseThrow().getCashBalance());

        TransferReceipt receipt = workflow.transfer(ana, "it-transfer-ok", new BigDecimal("30.00"), "10000002");
        TransferReceipt replay = workflow.transfer(ana, "it-transfer-ok", new BigDecimal("30.00"), "10000002");
        assertEquals(receipt.transferId(), replay.transferId());
        assertEquals(new BigDecimal("50.00"), accounts.findById("10000001").orElseThrow().getCashBalance());
        assertEquals(new BigDecimal("30.00"), accounts.findById("10000002").orElseThrow().getCashBalance());
    }

    private static AccountOpenedPayload opened(String number, String subject, String email, String name) {
        return new AccountOpenedPayload(number, subject, email, name, "2000.00", Money.CURRENCY, Instant.parse("2026-11-24T12:00:00Z"));
    }

    @TestConfiguration
    @EntityScan(basePackageClasses = LedgerAccountEntity.class)
    @EnableJpaRepositories(basePackageClasses = LedgerAccountRepository.class)
    @Import({
            JpaLedgerStore.class,
            LedgerWorkflow.class,
            OutboxWriter.class,
            CommonConfig.class,
            JacksonAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    static class LedgerSlice {
    }
}
