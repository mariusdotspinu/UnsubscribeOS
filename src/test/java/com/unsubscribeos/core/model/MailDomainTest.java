package com.unsubscribeos.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailDomainTest {

    private static EmailMessage withUnsubscribe(String id, UnsubscribeInfo info) {
        return new EmailMessage(id, "S", "s@x.com", "x.com", "subj", "snip",
                Instant.ofEpochSecond(1), Optional.ofNullable(info));
    }

    @Test
    void bestUnsubscribePrefersOneClick() {
        UnsubscribeInfo plain = new UnsubscribeInfo(List.of("https://a"), List.of(), false);
        UnsubscribeInfo oneClick = new UnsubscribeInfo(List.of("https://b"), List.of(), true);
        MailDomain domain = new MailDomain("x.com", List.of(
                withUnsubscribe("1", plain), withUnsubscribe("2", oneClick)));

        assertTrue(domain.bestUnsubscribe().orElseThrow().oneClick());
    }

    @Test
    void bestUnsubscribeEmptyWhenNoneAvailable() {
        MailDomain domain = new MailDomain("x.com", List.of(withUnsubscribe("1", null)));
        assertTrue(domain.bestUnsubscribe().isEmpty());
    }

    @Test
    void countReflectsMessages() {
        MailDomain domain = new MailDomain("x.com", List.of(
                withUnsubscribe("1", null), withUnsubscribe("2", null)));
        assertEquals(2, domain.count());
    }
}
