package com.unsubscribeos.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.unsubscribeos.config.AppPaths;
import com.unsubscribeos.core.http.Json;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.TokenSet;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link AccountStore}: each account is AES-256-GCM encrypted under a per-install
 * master key, both written with owner-only permissions in the app config directory. This
 * keeps credentials and tokens out of plaintext on disk; a local attacker already running
 * as the user is trusted, matching a single-user desktop app's threat model.
 */
public final class EncryptedAccountStore implements AccountStore {

    private final SecretKey masterKey = loadOrCreateKey();

    @Override
    public Optional<Account> load(Provider provider) {
        Path file = AppPaths.tokens(provider.name());
        if (!Files.exists(file)) return Optional.empty();
        try {
            byte[] plain = Aes.decrypt(masterKey, Files.readAllBytes(file));
            return Optional.of(deserialize(provider, Json.parse(new String(plain))));
        } catch (Exception e) {
            return Optional.empty(); // unreadable store ⇒ treat as logged-out
        }
    }

    @Override
    public void save(Account account) {
        Path file = AppPaths.tokens(account.provider().name());
        writeLocked(file, Aes.encrypt(masterKey, serialize(account).getBytes()));
    }

    @Override
    public void clear(Provider provider) {
        try {
            Files.deleteIfExists(AppPaths.tokens(provider.name()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not clear account", e);
        }
    }

    // ---- remembered credentials (survive sign-out) --------------------------

    @Override
    public void saveCredentials(Provider provider, Credentials credentials) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientId", credentials.clientId());
        m.put("clientSecret", credentials.clientSecret());
        writeLocked(AppPaths.credentials(provider.name()), Aes.encrypt(masterKey, Json.write(m).getBytes()));
    }

    @Override
    public Optional<Credentials> loadCredentials(Provider provider) {
        Path file = AppPaths.credentials(provider.name());
        if (!Files.exists(file)) return Optional.empty();
        try {
            JsonNode n = Json.parse(new String(Aes.decrypt(masterKey, Files.readAllBytes(file))));
            String secret = n.hasNonNull("clientSecret") ? n.get("clientSecret").asText() : null;
            return Optional.of(new Credentials(n.path("clientId").asText(), secret));
        } catch (Exception e) {
            return Optional.empty(); // unreadable ⇒ treat as not remembered
        }
    }

    @Override
    public void clearCredentials(Provider provider) {
        try {
            Files.deleteIfExists(AppPaths.credentials(provider.name()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not clear credentials", e);
        }
    }

    // ---- (de)serialisation --------------------------------------------------

    private String serialize(Account a) {
        TokenSet t = a.tokens();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientId", a.clientId());
        m.put("clientSecret", a.clientSecret());
        m.put("access", t.accessToken());
        m.put("refresh", t.refreshToken());
        m.put("exp", t.expiresAt().getEpochSecond());
        return Json.write(m);
    }

    private Account deserialize(Provider provider, JsonNode n) {
        TokenSet tokens = new TokenSet(
                n.path("access").asText(),
                n.hasNonNull("refresh") ? n.get("refresh").asText() : null,
                Instant.ofEpochSecond(n.path("exp").asLong()));
        String secret = n.hasNonNull("clientSecret") ? n.get("clientSecret").asText() : null;
        return new Account(provider, n.path("clientId").asText(), secret, tokens);
    }

    // ---- key + file handling ------------------------------------------------

    private SecretKey loadOrCreateKey() {
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

    private void writeLocked(Path file, byte[] bytes) {
        try {
            Files.write(file, bytes);
            AppPaths.trySetOwnerOnly(file);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write " + file, e);
        }
    }
}
