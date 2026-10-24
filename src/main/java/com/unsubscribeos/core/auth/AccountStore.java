package com.unsubscribeos.core.auth;

import com.unsubscribeos.config.AppPaths;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.TokenSet;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Persists signed-in accounts and remembered credentials, each AES-256-GCM encrypted under a
 * per-install master key, all written owner-only in the app config directory. This keeps
 * credentials and tokens out of plaintext on disk; a local attacker already running as the user is
 * trusted, matching a single-user desktop app's threat model. Remembered credentials live in their
 * own file so they survive sign-out, which clears only the session.
 */
public final class AccountStore {

    /** The session as stored on disk; the component names are the persisted JSON keys. */
    private record Session(String clientId, String clientSecret, String access, String refresh, long exp) {}

    private final SecretKey masterKey = loadOrCreateKey();

    public Optional<Account> load(Provider provider) {
        return read(AppPaths.tokens(provider), Session.class).map(s -> new Account(provider, s.clientId(),
                s.clientSecret(), new TokenSet(s.access(), s.refresh(), Instant.ofEpochSecond(s.exp()))));
    }

    public void save(Account account) {
        TokenSet t = account.tokens();
        write(AppPaths.tokens(account.provider()), new Session(account.clientId(), account.clientSecret(),
                t.accessToken(), t.refreshToken(), t.expiresAt().getEpochSecond()));
    }

    public void clear(Provider provider) {
        delete(AppPaths.tokens(provider));
    }

    public Optional<Credentials> loadCredentials(Provider provider) {
        return read(AppPaths.credentials(provider), Credentials.class);
    }

    public void saveCredentials(Provider provider, Credentials credentials) {
        write(AppPaths.credentials(provider), credentials);
    }

    public void clearCredentials(Provider provider) {
        delete(AppPaths.credentials(provider));
    }

    private <T> Optional<T> read(Path file, Class<T> type) {
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Json.read(new String(Aes.decrypt(masterKey, Files.readAllBytes(file))), type));
        } catch (Exception e) {
            return Optional.empty(); // unreadable ⇒ treat as signed out / not remembered
        }
    }

    private void write(Path file, Object value) {
        writeLocked(file, Aes.encrypt(masterKey, Json.write(value).getBytes()));
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + file, e);
        }
    }

    private static SecretKey loadOrCreateKey() {
        Path keyFile = AppPaths.keyFile();
        try {
            if (Files.exists(keyFile)) {
                return Aes.keyFrom(Base64.getDecoder().decode(Files.readString(keyFile).trim()));
            }
            SecretKey key = Aes.newKey();
            writeLocked(keyFile, Base64.getEncoder().encode(key.getEncoded()));
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialise encryption key", e);
        }
    }

    private static void writeLocked(Path file, byte[] bytes) {
        try {
            Files.write(file, bytes);
            AppPaths.trySetOwnerOnly(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }
}
