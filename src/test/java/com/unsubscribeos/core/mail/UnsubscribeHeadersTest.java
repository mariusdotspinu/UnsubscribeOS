package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.UnsubscribeInfo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsubscribeHeadersTest {

    @Test
    void parsesBracketedHttpAndMailto() {
        UnsubscribeInfo info = UnsubscribeHeaders.parse(
                "<https://x.com/u>, <mailto:u@x.com>", null, "x.com").orElseThrow();
        assertEquals("https://x.com/u", info.firstHttpUrl().orElseThrow());
        assertEquals("mailto:u@x.com", info.firstMailto().orElseThrow());
    }

    @Test
    void detectsOneClickPost() {
        assertTrue(UnsubscribeHeaders.parse("<https://x.com/u>", "List-Unsubscribe=One-Click", "x.com")
                .orElseThrow().oneClick());
    }

    @Test
    void parsesBareValueWithoutAngleBrackets() {
        // Non-RFC-compliant senders (the cause of "has unsubscribe but app misses it") omit the <>.
        UnsubscribeInfo info = UnsubscribeHeaders.parse("https://x.com/unsub?token=abc", null, "x.com")
                .orElseThrow();
        assertEquals("https://x.com/unsub?token=abc", info.firstHttpUrl().orElseThrow());
    }

    @Test
    void parsesBareCommaSeparatedValue() {
        UnsubscribeInfo info = UnsubscribeHeaders.parse(
                "https://x.com/unsub, mailto:u@x.com", null, "x.com").orElseThrow();
        assertEquals("https://x.com/unsub", info.firstHttpUrl().orElseThrow());
        assertEquals("mailto:u@x.com", info.firstMailto().orElseThrow());
    }

    @Test
    void emptyWhenHeaderMissing() {
        assertTrue(UnsubscribeHeaders.parse(null, null, "x.com").isEmpty());
        assertTrue(UnsubscribeHeaders.parse("  ", null, "x.com").isEmpty());
    }

    @Test
    void emptyWhenNoUsableUri() {
        assertTrue(UnsubscribeHeaders.parse("not-a-uri", null, "x.com").isEmpty());
    }

    @Test
    void carriesDomain() {
        UnsubscribeInfo info = UnsubscribeHeaders.parse("<https://x.com/u>", null, "elfster.com")
                .orElseThrow();
        assertEquals("elfster.com", info.domain());
        assertFalse(info.oneClick());
    }
}
