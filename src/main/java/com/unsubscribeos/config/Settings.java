package com.unsubscribeos.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class Settings {

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
        }
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "UnsubscribeOS settings");
        } catch (IOException ignored) {
        }
    }
}
