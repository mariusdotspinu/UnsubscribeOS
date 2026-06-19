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

    /** How many newest messages the initial mailbox scan reads (see {@link #scanDepth()}). */
    public static final String SCAN_DEPTH = "scan.depth";
    public static final int DEFAULT_SCAN_DEPTH = 5000;
    private static final int MIN_SCAN_DEPTH = 100;

    private final Properties props = new Properties();
    private final Path file = AppPaths.settings();

    public Settings() {
        load();
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(props.getProperty(key));
    }

    /**
     * Newest messages to scan on the initial fetch. Higher finds more senders but scans longer.
     * Tunable by editing {@code scan.depth} in settings.properties; falls back to the default when
     * unset or unparseable, and is floored so a typo can't disable scanning entirely.
     */
    public int scanDepth() {
        return get(SCAN_DEPTH)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Math.max(MIN_SCAN_DEPTH, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        return DEFAULT_SCAN_DEPTH;
                    }
                })
                .orElse(DEFAULT_SCAN_DEPTH);
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
