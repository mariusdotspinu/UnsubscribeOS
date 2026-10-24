package com.unsubscribeos.core.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record MailDomain(String domain, List<EmailMessage> messages) {

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

    public Optional<UnsubscribeInfo> bestUnsubscribe() {
        return messages.stream()
                .map(EmailMessage::unsubscribe)
                .flatMap(Optional::stream)
                .max(Comparator.comparing(UnsubscribeInfo::oneClick));
    }
}
