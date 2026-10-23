package com.unsubscribeos.ui.view.dashboard;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.MailDomain;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainSortTest {

    private static final List<MailDomain> DOMAINS =
            List.of(domain("a.com", 2), domain("b.com", 5), domain("c.com", 1));

    private static MailDomain domain(String name, int count) {
        List<EmailMessage> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(new EmailMessage(name + i, "n", "n@" + name, name, "s", "snip",
                    Instant.EPOCH, Optional.empty()));
        }
        return new MailDomain(name, messages);
    }

    private static List<String> sortedNames(DomainSort order) {
        return DOMAINS.stream().sorted(order.comparator()).map(MailDomain::domain).toList();
    }

    @Test
    void mostEmailsFirst() {
        assertEquals(List.of("b.com", "a.com", "c.com"), sortedNames(DomainSort.MOST_EMAILS));
    }

    @Test
    void nameAscending() {
        assertEquals(List.of("a.com", "b.com", "c.com"), sortedNames(DomainSort.NAME_ASC));
    }

    @Test
    void nameDescending() {
        assertEquals(List.of("c.com", "b.com", "a.com"), sortedNames(DomainSort.NAME_DESC));
    }
}
