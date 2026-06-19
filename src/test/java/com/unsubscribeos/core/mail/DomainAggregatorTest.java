package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.MailDomain;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainAggregatorTest {

    private static EmailMessage msg(String id, String domain, Instant date) {
        return new EmailMessage(id, "Sender", "s@" + domain, domain, "Subject " + id, "snippet", date, Optional.empty(), true);
    }

    @Test
    void groupsByDomainBusiestFirst() {
        List<MailDomain> domains = DomainAggregator.group(List.of(
                msg("1", "shop.com", Instant.ofEpochSecond(10)),
                msg("2", "shop.com", Instant.ofEpochSecond(20)),
                msg("3", "news.com", Instant.ofEpochSecond(30))));

        assertEquals(2, domains.size());
        assertEquals("shop.com", domains.get(0).domain());
        assertEquals(2, domains.get(0).count());
        assertEquals("news.com", domains.get(1).domain());
    }

    @Test
    void sortsMessagesNewestFirstWithinDomain() {
        List<MailDomain> domains = DomainAggregator.group(List.of(
                msg("old", "shop.com", Instant.ofEpochSecond(10)),
                msg("new", "shop.com", Instant.ofEpochSecond(99))));

        assertEquals(List.of("new", "old"), domains.get(0).messageIds());
    }

    @Test
    void tiedDomainsSortAlphabetically() {
        List<MailDomain> domains = DomainAggregator.group(List.of(
                msg("1", "b.com", Instant.ofEpochSecond(1)),
                msg("2", "a.com", Instant.ofEpochSecond(2))));

        assertEquals("a.com", domains.get(0).domain());
        assertEquals("b.com", domains.get(1).domain());
    }
}
