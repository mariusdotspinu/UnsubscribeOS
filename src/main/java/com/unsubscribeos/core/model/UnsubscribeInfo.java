package com.unsubscribeos.core.model;

import java.util.List;
import java.util.Optional;

public record UnsubscribeInfo(List<String> httpUrls, List<String> mailtoUrls, boolean oneClick, String domain) {

    public Optional<String> firstHttpUrl() {
        return httpUrls.stream().findFirst();
    }

    public Optional<String> firstMailto() {
        return mailtoUrls.stream().findFirst();
    }
}
