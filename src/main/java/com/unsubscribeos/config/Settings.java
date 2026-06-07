package com.unsubscribeos.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/** Tiny key/value preferences backed by {@code settings.properties} (theme, last provider). */
public final class Settings {

    private final Properties props = new Properties();
    private final Path file = AppPaths.settings();

    public Settings() {
        load();
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(props.getProperty(key));
    }

    public void put(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) {
            // start with defaults if unreadable
        }
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "UnsubscribeOS settings");
        } catch (IOException ignored) {
            // non-fatal: preferences just won't persist this run
        }
    }
}
