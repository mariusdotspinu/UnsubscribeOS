package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.MailDomain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Groups messages by sender domain, newest-first within a domain and busiest domain first. */
public final class DomainAggregator {

    private DomainAggregator() {}

    public static List<MailDomain> group(Collection<EmailMessage> messages) {
        return messages.stream()
                .collect(Collectors.groupingBy(EmailMessage::domain))
                .entrySet().stream()
                .map(e -> new MailDomain(e.getKey(), sortNewestFirst(e.getValue())))
                .sorted(Comparator.comparingInt(MailDomain::count).reversed()
                        .thenComparing(MailDomain::domain))
                .toList();
    }

    private static List<EmailMessage> sortNewestFirst(List<EmailMessage> messages) {
        return messages.stream()
                .sorted(Comparator.comparing(EmailMessage::date).reversed())
                .toList();
    }
}
