package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Callbacks a {@code MailService} uses to stream results to the UI: each parsed message,
 * periodic progress, a cancellation probe so a sign-out or window close stops fetching, and a
 * filter so live re-polls only fetch the bodies of messages not already seen.
 */
public record FetchContext(
        Consumer<EmailMessage> onMessage,
        Consumer<FetchProgress> onProgress,
        BooleanSupplier cancelled,
        Predicate<String> shouldFetch) {

    public boolean isCancelled() {
        return cancelled.getAsBoolean();
    }
}
