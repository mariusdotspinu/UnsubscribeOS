package com.unsubscribeos.core.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All messages received from a single sender domain, newest first. The aggregate the dashboard
 * groups by, sorts and colour-grades by volume.
 */
public record MailDomain(String domain, List<EmailMessage> messages) {

    /** One domain per sender domain; the caller picks the order of the domains. */
    public static List<MailDomain> group(Collection<EmailMessage> messages) {
        return messages.stream()
                .collect(Collectors.groupingBy(EmailMessage::domain))
                .entrySet().stream()
                .map(e -> new MailDomain(e.getKey(), e.getValue().stream()
                        .sorted(Comparator.comparing(EmailMessage::date).reversed())
                        .toList()))
                .toList();
    }

    public int count() {
        return messages.size();
    }

    /** Any message that exposes unsubscribe instructions, preferring one-click senders. */
    public Optional<UnsubscribeInfo> bestUnsubscribe() {
        return messages.stream()
                .map(EmailMessage::unsubscribe)
                .flatMap(Optional::stream)
                .max(Comparator.comparing(UnsubscribeInfo::oneClick));
    }
}
