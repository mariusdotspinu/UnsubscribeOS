package com.unsubscribeos.core.model;

/**
 * A supported mail provider. Adding a provider here (plus a {@code MailService}
 * implementation and a {@code ProviderConfigs} entry) is all that is required to
 * extend UnsubscribeOS.
 */
public enum Provider {
    GMAIL("Gmail", "📮"),
    OUTLOOK("Outlook", "📨");

    private final String displayName;
    private final String glyph;

    Provider(String displayName, String glyph) {
        this.displayName = displayName;
        this.glyph = glyph;
    }

    public String displayName() { return displayName; }
    public String glyph() { return glyph; }
}
