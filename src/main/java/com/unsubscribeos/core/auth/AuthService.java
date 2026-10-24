package com.unsubscribeos.core.auth;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.TokenSet;

import java.util.Optional;

/**
 * Orchestrates the account lifecycle: interactive sign-in, transparent token refresh,
 * persistence and sign-out. UI and mail layers depend only on this, never on the OAuth
 * mechanics or the store implementation (dependency inversion).
 */
public final class AuthService {

    private final AccountStore store = new AccountStore();

    /** Interactive browser sign-in. Blocking — run off the UI thread. */
    public Account signIn(Provider provider, String clientId, String clientSecret, boolean remember) {
        TokenSet tokens = new LoopbackOAuth(provider.oauth(clientId, clientSecret)).authorize();
        Account account = new Account(provider, clientId, clientSecret, tokens);
        store.save(account);
        if (remember) store.saveCredentials(provider, new Credentials(clientId, clientSecret));
        else store.clearCredentials(provider);
        return account;
    }

    /** Previously remembered credentials for a provider, if "Remember me" was chosen. */
    public Optional<Credentials> rememberedCredentials(Provider provider) {
        return store.loadCredentials(provider);
    }

    /** Restores a previously signed-in account, refreshing its access token if stale. */
    public Optional<Account> restore(Provider provider) {
        return store.load(provider).map(this::ensureFresh);
    }

    /** Returns a valid access token for the account, refreshing and persisting if needed. */
    public String accessToken(Account account) {
        return ensureFresh(account).tokens().accessToken();
    }

    /** Signs out of the session (clears tokens). Remembered credentials are kept for a quick re-login. */
    public void signOut(Provider provider) {
        store.clear(provider);
    }

    private Account ensureFresh(Account account) {
        if (!account.tokens().isExpired()) return account;
        TokenSet refreshed = new LoopbackOAuth(account.provider().oauth(account.clientId(), account.clientSecret()))
                .refresh(account.tokens().refreshToken())
                .mergeRefreshToken(account.tokens());
        Account updated = account.withTokens(refreshed);
        store.save(updated);
        return updated;
    }
}
