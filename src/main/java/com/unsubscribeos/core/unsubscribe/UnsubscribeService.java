package com.unsubscribeos.core.unsubscribe;

import com.unsubscribeos.core.http.Http;
import com.unsubscribeos.core.model.UnsubscribeInfo;
import com.unsubscribeos.core.platform.Browser;

import java.util.Optional;

public final class UnsubscribeService {

    private static final String ONE_CLICK_BODY = "List-Unsubscribe=One-Click";
    private static final String FORM_TYPE = "application/x-www-form-urlencoded";

    public UnsubscribeResult unsubscribe(Optional<UnsubscribeInfo> found) {
        if (found.isEmpty()) return new UnsubscribeResult(false, "This sender did not provide unsubscribe information.");
        UnsubscribeInfo info = found.get();
        String domain = info.domain();
        Optional<String> page = info.firstHttpUrl();
        if (page.isPresent() && info.oneClick()) return oneClick(page.get(), domain);
        if (page.isPresent()) return open(page.get(), "Opened the unsubscribe page for " + domain + " in your browser.");
        return info.firstMailto()
                .map(uri -> open(uri, "Opened a pre-filled unsubscribe email for " + domain + "."))
                .orElse(new UnsubscribeResult(false, "No usable unsubscribe method."));
    }

    private UnsubscribeResult oneClick(String url, String domain) {
        try {
            int code = Http.postRaw(url, FORM_TYPE, ONE_CLICK_BODY);
            return code >= 200 && code < 400
                    ? new UnsubscribeResult(true, "Unsubscribed from " + domain + " in one click.")
                    : open(url, "Couldn't auto-unsubscribe from " + domain + " (HTTP " + code + ") — opened the page instead.");
        } catch (RuntimeException e) {
            return open(url, "Couldn't auto-unsubscribe from " + domain + " — opened the page instead.");
        }
    }

    private UnsubscribeResult open(String uri, String detail) {
        try {
            Browser.open(uri);
            return new UnsubscribeResult(true, detail);
        } catch (RuntimeException e) {
            return new UnsubscribeResult(false, e.getMessage());
        }
    }
}
