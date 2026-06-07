package com.unsubscribeos.ui.component;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialTest {

    @Test
    void extractsUrlsAndStripsTrailingPunctuation() {
        List<String> urls = Tutorial.urls(
                "Open  https://console.cloud.google.com/  then visit https://entra.microsoft.com/.");
        assertEquals(
                List.of("https://console.cloud.google.com/", "https://entra.microsoft.com/"),
                urls);
    }

    @Test
    void dedupesRepeatedUrls() {
        assertEquals(List.of("https://x.com"), Tutorial.urls("a https://x.com b https://x.com"));
    }

    @Test
    void returnsEmptyWhenNoUrls() {
        assertTrue(Tutorial.urls("no links in this text").isEmpty());
    }
}
