package com.unsubscribeos.core.model;

import java.util.List;

/**
 * A supported mail provider: its OAuth endpoints and scopes, plus the help shown while the user
 * creates their own OAuth client. Adding a provider is an entry here and a {@code MailService}.
 */
public enum Provider {
    GMAIL("Gmail",
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://oauth2.googleapis.com/token",
            List.of("https://www.googleapis.com/auth/gmail.modify"),
            true, // Google "Desktop app" clients issue a (non-confidential) secret.
            """
            To sign in with your own Gmail account you create a free OAuth client in Google Cloud:

            1.  Open  https://console.cloud.google.com/  and create (or pick) a project.
            2.  APIs & Services ▸ Library ▸ enable the "Gmail API".
            3.  APIs & Services ▸ OAuth consent screen ▸ choose "External", add yourself as a Test user.
            4.  APIs & Services ▸ Credentials ▸ Create credentials ▸ OAuth client ID.
            5.  Application type: "Desktop app". Create.
            6.  Copy the Client ID and Client secret into the fields below.

            UnsubscribeOS only ever talks to Google directly from your machine — your
            credentials never leave this computer."""),
    OUTLOOK("Outlook",
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            List.of("offline_access", "https://graph.microsoft.com/Mail.ReadWrite"),
            false,
            """
            To sign in with your own Outlook / Microsoft account you register a free app:

            1.  Open  https://entra.microsoft.com/  ▸ App registrations ▸ New registration.
            2.  Supported account types: "Personal Microsoft accounts" (and/or org accounts).
            3.  Redirect URI: platform "Mobile and desktop applications", value http://localhost.
            4.  After creating, copy the "Application (client) ID".
            5.  API permissions ▸ Microsoft Graph ▸ Delegated ▸ add Mail.ReadWrite and offline_access.
            6.  Paste the Client ID below. No client secret is needed for desktop sign-in.

            UnsubscribeOS talks to Microsoft Graph directly from your machine only.""");

    private final String displayName;
    private final String authEndpoint;
    private final String tokenEndpoint;
    private final List<String> scopes;
    private final boolean requiresSecret;
    private final String helpText;

    Provider(String displayName, String authEndpoint, String tokenEndpoint, List<String> scopes,
             boolean requiresSecret, String helpText) {
        this.displayName = displayName;
        this.authEndpoint = authEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.scopes = scopes;
        this.requiresSecret = requiresSecret;
        this.helpText = helpText;
    }

    public String displayName()     { return displayName; }
    public boolean requiresSecret() { return requiresSecret; }
    public String helpText()        { return helpText; }

    /** This provider's endpoints combined with the user's own OAuth client credentials. */
    public OAuthConfig oauth(String clientId, String clientSecret) {
        return new OAuthConfig(clientId, clientSecret, authEndpoint, tokenEndpoint, scopes);
    }
}
