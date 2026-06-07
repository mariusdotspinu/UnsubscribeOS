package com.unsubscribeos.core.model;

import java.util.List;
import java.util.Optional;

/**
 * Parsed RFC 2369 / RFC 8058 unsubscribe instructions. {@code oneClick} indicates the
 * sender supports List-Unsubscribe-Post one-click unsubscribe via an HTTPS POST.
 */
public record UnsubscribeInfo(List<String> httpUrls, List<String> mailtoUrls, boolean oneClick) {

    public Optional<String> firstHttpUrl() {
        return httpUrls.stream().findFirst();
    }

    public Optional<String> firstMailto() {
        return mailtoUrls.stream().findFirst();
    }
}
