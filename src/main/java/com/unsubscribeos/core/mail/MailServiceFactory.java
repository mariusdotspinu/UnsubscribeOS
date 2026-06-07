package com.unsubscribeos.core.mail;

import com.unsubscribeos.core.mail.gmail.GmailService;
import com.unsubscribeos.core.mail.outlook.OutlookService;
import com.unsubscribeos.core.model.Provider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Resolves the {@link MailService} for a provider. The single switch point for wiring a
 * new provider implementation into the app.
 */
public final class MailServiceFactory {

    private static final Map<Provider, Supplier<MailService>> REGISTRY = Map.of(
            Provider.GMAIL, GmailService::new,
            Provider.OUTLOOK, OutlookService::new);

    private MailServiceFactory() {}

    public static MailService create(Provider provider) {
        Supplier<MailService> supplier = REGISTRY.get(provider);
        if (supplier == null) throw new IllegalArgumentException("Unsupported provider: " + provider);
        return supplier.get();
    }
}
