package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.EmailMessage;
import com.unsubscribeos.core.model.FetchProgress;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public record FetchContext(
        Consumer<EmailMessage> onMessage,
        Consumer<FetchProgress> onProgress,
        BooleanSupplier cancelled,
        Predicate<String> shouldFetch) {

    public boolean isCancelled() {
        return cancelled.getAsBoolean();
    }
}
