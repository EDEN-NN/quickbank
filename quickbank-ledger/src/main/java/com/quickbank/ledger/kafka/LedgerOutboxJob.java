package com.quickbank.ledger.kafka;

import com.quickbank.common.outbox.OutboxRelay;
import com.quickbank.common.outbox.OutboxTables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxJob {

    private final OutboxRelay relay;
    private final int batchSize;

    public LedgerOutboxJob(OutboxRelay relay, @Value("${quickbank.outbox.batch-size:40}") int batchSize) {
        this.relay = relay;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${quickbank.outbox.delay-ms:250}")
    public void flush() {
        relay.flush(OutboxTables.LEDGER, batchSize);
    }
}
