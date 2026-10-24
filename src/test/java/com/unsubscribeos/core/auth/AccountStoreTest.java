package com.unsubscribeos.core.auth;

import com.unsubscribeos.config.AppPaths;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Credentials;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.model.TokenSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountStoreTest {

    @TempDir
    static Path home;

    @BeforeAll
    static void isolateAppDir() {
        // AppPaths reads this once, so it must be set before AppPaths is first touched.
        System.setProperty("unsubscribeos.home", home.toString());
    }

    @Test
    void sessionRoundTripsAndClears() {
        AccountStore store = new AccountStore();
        Account account = new Account(Provider.OUTLOOK, "id", null,
                new TokenSet("access", "refresh", Instant.ofEpochSecond(1_800_000_000L)));

        store.save(account);
        assertEquals(account, store.load(Provider.OUTLOOK).orElseThrow());

        store.clear(Provider.OUTLOOK);
        assertTrue(store.load(Provider.OUTLOOK).isEmpty());
    }

    @Test
    void credentialsRoundTrip() {
        AccountStore store = new AccountStore();
        store.saveCredentials(Provider.GMAIL, new Credentials("id", "secret"));
        assertEquals(new Credentials("id", "secret"), store.loadCredentials(Provider.GMAIL).orElseThrow());
    }

    @Test
    void readsSessionsWrittenByEarlierVersions() throws Exception {
        AccountStore store = new AccountStore(); // creates the master key
        var key = Aes.keyFrom(Base64.getDecoder().decode(Files.readString(AppPaths.keyFile()).trim()));
        String legacy = "{\"clientId\":\"id\",\"clientSecret\":\"s\",\"access\":\"a\",\"refresh\":null,\"exp\":1800000000}";
        Files.write(AppPaths.tokens(Provider.GMAIL), Aes.encrypt(key, legacy.getBytes()));

        assertEquals(new Account(Provider.GMAIL, "id", "s", new TokenSet("a", null, Instant.ofEpochSecond(1_800_000_000L))),
                store.load(Provider.GMAIL).orElseThrow());
    }
}
