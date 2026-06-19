package com.unsubscribeos.core.unsubscribe;

import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.model.UnsubscribeInfo;
import com.unsubscribeos.core.platform.Browser;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Executes List-Unsubscribe instructions, preferring the safest automatic path. The candidate
 * methods are an ordered list of strategies (Strategy pattern): the first one that applies to the
 * message wins — a silent RFC 8058 one-click HTTPS POST, else the unsubscribe web page, else a
 * pre-filled mail draft. Provider-independent by design.
 */
public final class UnsubscribeService {

    private static final String ONE_CLICK_BODY = "List-Unsubscribe=One-Click";
    private static final String FORM_TYPE = "application/x-www-form-urlencoded";

    /** Ordered, most-preferred first. Each returns a result only when it applies to the message. */
    private final List<Function<UnsubscribeInfo, Optional<UnsubscribeResult>>> strategies = List.of(
            this::oneClickStrategy, this::browserStrategy, this::mailtoStrategy);

    public UnsubscribeResult unsubscribe(Optional<UnsubscribeInfo> info) {
        return info.map(this::unsubscribe).orElseGet(() -> UnsubscribeResult.of(
                UnsubscribeResult.Status.UNAVAILABLE, "This sender did not provide unsubscribe information."));
    }

    public UnsubscribeResult unsubscribe(UnsubscribeInfo info) {
        return strategies.stream()
                .flatMap(strategy -> strategy.apply(info).stream())
                .findFirst()
                .orElseGet(() -> UnsubscribeResult.of(
                        UnsubscribeResult.Status.UNAVAILABLE, "No usable unsubscribe method."));
    }

    // ---- strategies ---------------------------------------------------------

    private Optional<UnsubscribeResult> oneClickStrategy(UnsubscribeInfo info) {
        if (!info.oneClick()) return Optional.empty();
        return info.firstHttpUrl().map(url -> oneClick(url, info.domain()));
    }

    private Optional<UnsubscribeResult> browserStrategy(UnsubscribeInfo info) {
        return info.firstHttpUrl().map(url -> open(url, UnsubscribeResult.Status.OPENED_BROWSER,
                "Opened the unsubscribe page for " + info.domain() + " in your browser."));
    }

    private Optional<UnsubscribeResult> mailtoStrategy(UnsubscribeInfo info) {
        return info.firstMailto().map(uri -> open(uri, UnsubscribeResult.Status.OPENED_MAIL,
                "Opened a pre-filled unsubscribe email for " + info.domain() + "."));
    }

    // ---- execution ----------------------------------------------------------

    private UnsubscribeResult oneClick(String url, String domain) {
        try {
            int code = Http.postRaw(url, FORM_TYPE, ONE_CLICK_BODY);
            return code >= 200 && code < 400
                    ? UnsubscribeResult.of(UnsubscribeResult.Status.ONE_CLICK,
                            "Unsubscribed from " + domain + " in one click.")
                    : open(url, UnsubscribeResult.Status.OPENED_BROWSER,
                            "Couldn't auto-unsubscribe from " + domain + " (HTTP " + code + ") — opened the page instead.");
        } catch (RuntimeException e) {
            return open(url, UnsubscribeResult.Status.OPENED_BROWSER,
                    "Couldn't auto-unsubscribe from " + domain + " — opened the page instead.");
        }
    }

    private UnsubscribeResult open(String uri, UnsubscribeResult.Status status, String detail) {
        try {
            Browser.open(uri);
            return UnsubscribeResult.of(status, detail);
        } catch (RuntimeException e) {
            return UnsubscribeResult.of(UnsubscribeResult.Status.FAILED, e.getMessage());
        }
    }
}
