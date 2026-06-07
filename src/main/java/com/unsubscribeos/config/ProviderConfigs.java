package com.unsubscribeos.config;

import com.unsubscribeos.core.model.OAuthConfig;
import com.unsubscribeos.core.model.Provider;

import java.util.List;

/**
 * Static OAuth endpoint/scope definitions per provider, plus the human-readable
 * instructions shown in the credentials help panel. User-supplied client id/secret
 * are injected at sign-in time via {@link #oauthConfig}.
 */
public final class ProviderConfigs {

    private ProviderConfigs() {}

    public static OAuthConfig oauthConfig(Provider provider, String clientId, String clientSecret) {
        return switch (provider) {
            case GMAIL -> new OAuthConfig(
                    clientId, clientSecret,
                    "https://accounts.google.com/o/oauth2/v2/auth",
                    "https://oauth2.googleapis.com/token",
                    List.of("https://www.googleapis.com/auth/gmail.modify"));
            case OUTLOOK -> new OAuthConfig(
                    clientId, clientSecret,
                    "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                    "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                    List.of("offline_access", "https://graph.microsoft.com/Mail.ReadWrite"));
        };
    }

    /** True when the provider requires the user to also supply a client secret. */
    public static boolean requiresSecret(Provider provider) {
        return provider == Provider.GMAIL; // Google "Desktop app" clients issue a (non-confidential) secret.
    }

    public static String helpText(Provider provider) {
        return switch (provider) {
            case GMAIL -> """
                    To sign in with your own Gmail account you create a free OAuth client in Google Cloud:

                    1.  Open  https://console.cloud.google.com/  and create (or pick) a project.
                    2.  APIs & Services ▸ Library ▸ enable the "Gmail API".
                    3.  APIs & Services ▸ OAuth consent screen ▸ choose "External", add yourself as a Test user.
                    4.  APIs & Services ▸ Credentials ▸ Create credentials ▸ OAuth client ID.
                    5.  Application type: "Desktop app". Create.
                    6.  Copy the Client ID and Client secret into the fields below.

                    UnsubscribeOS only ever talks to Google directly from your machine — your
                    credentials never leave this computer.""";
            case OUTLOOK -> """
                    To sign in with your own Outlook / Microsoft account you register a free app:

                    1.  Open  https://entra.microsoft.com/  ▸ App registrations ▸ New registration.
                    2.  Supported account types: "Personal Microsoft accounts" (and/or org accounts).
                    3.  Redirect URI: platform "Mobile and desktop applications", value http://localhost.
                    4.  After creating, copy the "Application (client) ID".
                    5.  API permissions ▸ Microsoft Graph ▸ Delegated ▸ add Mail.ReadWrite and offline_access.
                    6.  Paste the Client ID below. No client secret is needed for desktop sign-in.

                    UnsubscribeOS talks to Microsoft Graph directly from your machine only.""";
        };
    }
}
