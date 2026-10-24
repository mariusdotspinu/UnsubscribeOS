package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.UnsubscribeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnsubscribeHeaders {

    private static final Pattern URI = Pattern.compile("<([^>]+)>");

    private UnsubscribeHeaders() {}

    public static Optional<UnsubscribeInfo> parse(String listUnsubscribe, String listUnsubscribePost, String domain) {
        if (listUnsubscribe == null || listUnsubscribe.isBlank()) return Optional.empty();

        List<String> http = new ArrayList<>();
        List<String> mailto = new ArrayList<>();
        Matcher m = URI.matcher(listUnsubscribe);
        boolean bracketed = false;
        while (m.find()) {
            bracketed = true;
            classify(m.group(1).trim(), http, mailto);
        }
        // Some senders omit RFC 2369's angle brackets; accept bare or comma-separated URIs.
        if (!bracketed) {
            for (String token : listUnsubscribe.split("[,\\s]+")) classify(token.trim(), http, mailto);
        }
        if (http.isEmpty() && mailto.isEmpty()) return Optional.empty();

        boolean oneClick = listUnsubscribePost != null
                && listUnsubscribePost.toLowerCase().contains("one-click");
        return Optional.of(new UnsubscribeInfo(List.copyOf(http), List.copyOf(mailto), oneClick, domain));
    }

    private static void classify(String uri, List<String> http, List<String> mailto) {
        if (uri.regionMatches(true, 0, "mailto:", 0, 7)) mailto.add(uri);
        else if (uri.regionMatches(true, 0, "http", 0, 4)) http.add(uri);
    }
}
