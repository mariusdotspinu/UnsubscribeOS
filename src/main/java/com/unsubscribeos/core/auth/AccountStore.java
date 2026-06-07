package com.unsubscribeos.core.auth;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;

import java.util.Optional;

/**
 * Persistence boundary for signed-in accounts (credentials + tokens). Abstracted so the
 * default encrypted-file implementation can later be swapped for an OS keychain. Remembered
 * credentials live separately so they survive sign-out (which clears the session account).
 */
public interface AccountStore {

    Optional<Account> load(Provider provider);

    void save(Account account);

    void clear(Provider provider);

    void saveCredentials(Provider provider, Credentials credentials);

    Optional<Credentials> loadCredentials(Provider provider);

    void clearCredentials(Provider provider);
}
