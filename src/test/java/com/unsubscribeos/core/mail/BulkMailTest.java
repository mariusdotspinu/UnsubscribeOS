package com.unsubscribeos.core.mail;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkMailTest {

    @Test
    void personalMailIsNotBulk() {
        assertFalse(BulkMail.isBulk(Map.of("from", "a@b.com", "subject", "lunch?")));
        assertFalse(BulkMail.isBulk(Map.of()));
    }

    @Test
    void listAndAutomationHeadersMarkBulk() {
        assertTrue(BulkMail.isBulk(Map.of("list-unsubscribe", "<https://x/u>")));
        assertTrue(BulkMail.isBulk(Map.of("list-id", "<news.x.com>")));
        assertTrue(BulkMail.isBulk(Map.of("list-post", "<mailto:list@x>")));
        assertTrue(BulkMail.isBulk(Map.of("precedence", "bulk")));
        assertTrue(BulkMail.isBulk(Map.of("precedence", "List")));
        assertTrue(BulkMail.isBulk(Map.of("auto-submitted", "auto-generated")));
    }

    @Test
    void autoSubmittedNoIsNotBulk() {
        assertFalse(BulkMail.isBulk(Map.of("auto-submitted", "no")));
    }

    @Test
    void blankHeaderValuesAreIgnored() {
        assertFalse(BulkMail.isBulk(Map.of("list-unsubscribe", "  ")));
        assertFalse(BulkMail.isBulk(Map.of("precedence", "normal")));
    }
}
