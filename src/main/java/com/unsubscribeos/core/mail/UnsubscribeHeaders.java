package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.UnsubscribeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the RFC 2369 {@code List-Unsubscribe} header (one or more angle-bracketed URIs)
 * together with the RFC 8058 {@code List-Unsubscribe-Post} header that signals one-click
 * HTTPS unsubscribe support.
 */
public final class UnsubscribeHeaders {

    private static final Pattern URI = Pattern.compile("<([^>]+)>");

    private UnsubscribeHeaders() {}

    public static Optional<UnsubscribeInfo> parse(String listUnsubscribe, String listUnsubscribePost) {
        if (listUnsubscribe == null || listUnsubscribe.isBlank()) return Optional.empty();

        List<String> http = new ArrayList<>();
        List<String> mailto = new ArrayList<>();
        Matcher m = URI.matcher(listUnsubscribe);
        while (m.find()) {
            String uri = m.group(1).trim();
            if (uri.regionMatches(true, 0, "mailto:", 0, 7)) mailto.add(uri);
            else if (uri.regionMatches(true, 0, "http", 0, 4)) http.add(uri);
        }
        if (http.isEmpty() && mailto.isEmpty()) return Optional.empty();

        boolean oneClick = listUnsubscribePost != null
                && listUnsubscribePost.toLowerCase().contains("one-click");
        return Optional.of(new UnsubscribeInfo(List.copyOf(http), List.copyOf(mailto), oneClick));
    }
}
