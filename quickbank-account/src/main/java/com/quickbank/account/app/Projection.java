package com.quickbank.account.app;

import com.quickbank.common.event.payload.SendEmailPayload;

import java.util.List;

public record Projection(SnapshotView snapshot, List<StatementLine> lines, List<SendEmailPayload> emails) {
}
