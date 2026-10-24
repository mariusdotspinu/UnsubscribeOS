package com.unsubscribeos.core.model;

import java.time.Instant;
import java.util.Optional;

public record EmailMessage(
        String id,
        String domain,
        String subject,
        String snippet,
        Instant date,
        Optional<UnsubscribeInfo> unsubscribe) {
}
