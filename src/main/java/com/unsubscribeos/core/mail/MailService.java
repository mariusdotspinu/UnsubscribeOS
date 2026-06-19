package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.model.Provider;

import java.util.List;

/**
 * Provider-agnostic mail operations. A new provider is added by implementing this
 * interface and registering it in {@link MailServiceFactory} — nothing else changes
 * (open/closed principle).
 */
public interface MailService {

    Provider provider();

    /**
     * Streams recent messages to the context's callbacks, scanning at most {@code maxMessages}
     * (newest first). Blocking — run off the UI thread.
     */
    void fetch(String accessToken, int maxMessages, FetchContext context);

    /** Moves the given messages to the provider's trash/deleted folder (recoverable). */
    void delete(String accessToken, List<String> messageIds);
}
