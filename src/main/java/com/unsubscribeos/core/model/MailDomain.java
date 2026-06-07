package com.unsubscribeos.core.model;

import java.util.List;
import java.util.Optional;

/**
 * All messages received from a single sender domain. The aggregate the dashboard
 * groups by, sorts by {@link #count()}, and colour-grades by volume.
 */
public record MailDomain(String domain, List<EmailMessage> messages) {

    public int count() {
        return messages.size();
    }

    public List<String> messageIds() {
        return messages.stream().map(EmailMessage::id).toList();
    }

    /** Any message that exposes unsubscribe instructions, preferring one-click senders. */
    public Optional<UnsubscribeInfo> bestUnsubscribe() {
        return messages.stream()
                .map(EmailMessage::unsubscribe)
                .flatMap(Optional::stream)
                .max((a, b) -> Boolean.compare(a.oneClick(), b.oneClick()));
    }
}
