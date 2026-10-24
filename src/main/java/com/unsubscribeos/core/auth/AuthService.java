package com.unsubscribeos.core.auth;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.TokenSet;

import java.util.Optional;

public final class AuthService {

    private final AccountStore store = new AccountStore();

    public Account signIn(Provider provider, String clientId, String clientSecret, boolean remember) {
        TokenSet tokens = new LoopbackOAuth(provider.oauth(clientId, clientSecret)).authorize();
        Account account = new Account(provider, clientId, clientSecret, tokens);
        store.save(account);
        if (remember) store.saveCredentials(provider, new Credentials(clientId, clientSecret));
        else store.clearCredentials(provider);
        return account;
    }

    public Optional<Credentials> rememberedCredentials(Provider provider) {
        return store.loadCredentials(provider);
    }

    public Optional<Account> restore(Provider provider) {
        return store.load(provider).map(this::ensureFresh);
    }

    public String accessToken(Account account) {
        return ensureFresh(account).tokens().accessToken();
    }

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
