package com.unsubscribeos.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailDomainGroupTest {

    private static EmailMessage msg(String id, String domain, Instant date) {
        return new EmailMessage(id, domain, "Subject " + id, "snippet", date, Optional.empty());
    }

    @Test
    void groupsByDomain() {
        List<MailDomain> domains = MailDomain.group(List.of(
                        msg("1", "shop.com", Instant.ofEpochSecond(10)),
                        msg("2", "shop.com", Instant.ofEpochSecond(20)),
                        msg("3", "news.com", Instant.ofEpochSecond(30))))
                .stream().sorted(Comparator.comparing(MailDomain::domain)).toList();

        assertEquals(List.of("news.com", "shop.com"), domains.stream().map(MailDomain::domain).toList());
        assertEquals(List.of(1, 2), domains.stream().map(MailDomain::count).toList());
    }

    @Test
    void sortsMessagesNewestFirstWithinDomain() {
        List<MailDomain> domains = MailDomain.group(List.of(
                msg("old", "shop.com", Instant.ofEpochSecond(10)),
                msg("new", "shop.com", Instant.ofEpochSecond(99))));

        assertEquals(List.of("new", "old"), domains.get(0).messages().stream().map(EmailMessage::id).toList());
    }
}
