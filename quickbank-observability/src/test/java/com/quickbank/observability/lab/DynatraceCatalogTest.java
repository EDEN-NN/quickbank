package com.quickbank.observability.lab;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DynatraceCatalogTest {

    @Test
    void queriesFileContainsEveryCatalogIdAndTheAccountAttribute() throws Exception {
        Path queries = Path.of("dynatrace/QUERIES.md");
        if (!Files.exists(queries)) {
            queries = Path.of("../dynatrace/QUERIES.md");
        }
        String markdown = Files.readString(queries);
        DynatraceCatalog catalog = new DynatraceCatalog();
        for (QuerySpec query : catalog.queries()) {
            assertTrue(markdown.contains("## " + query.id()), query.id());
            assertTrue(markdown.contains("account.number"), query.id());
        }
        for (SpanSpec span : catalog.spans()) {
            assertTrue(markdown.contains(span.name()), span.name());
        }
        assertTrue(markdown.contains("mock"));
        assertTrue(markdown.contains("transfer.id"));
        assertTrue(markdown.contains("movement.type"));
    }
}
