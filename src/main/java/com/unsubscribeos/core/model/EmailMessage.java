package com.unsubscribeos.core.model;

import java.time.Instant;
import java.util.Optional;

/**
 * A single mail message, normalised across providers. {@code unsubscribe} holds the parsed
 * contents of the RFC 2369 List-Unsubscribe header when the sender offers one — senders that do
 * get an unsubscribe button on the dashboard, the rest are delete-only.
 */
public record EmailMessage(
        String id,
        String fromName,
        String fromAddress,
        String domain,
        String subject,
        String snippet,
        Instant date,
        Optional<UnsubscribeInfo> unsubscribe) {

    public boolean canUnsubscribe() {
        return unsubscribe.isPresent();
    }
}
