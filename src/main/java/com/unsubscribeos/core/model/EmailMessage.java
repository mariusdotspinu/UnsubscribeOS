package com.unsubscribeos.core.model;

import java.time.Instant;
import java.util.Optional;

/**
 * A single mail message, normalised across providers. {@code unsubscribe} holds the parsed
 * contents of the RFC 2369 List-Unsubscribe header, when present. {@code bulk} marks automated /
 * list mail (newsletters, notifications, receipts) as opposed to personal correspondence, so the
 * dashboard can show senders worth cleaning up while leaving personal mail out of reach.
 */
public record EmailMessage(
        String id,
        String fromName,
        String fromAddress,
        String domain,
        String subject,
        String snippet,
        Instant date,
        Optional<UnsubscribeInfo> unsubscribe,
        boolean bulk) {

    public boolean canUnsubscribe() {
        return unsubscribe.isPresent();
    }
}
