package com.unsubscribeos.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Resolves and lazily creates the per-user application data directory and the files
 * UnsubscribeOS persists there (encrypted tokens, encryption key, settings).
 * The directory is locked down to the owner on POSIX systems.
 */
public final class AppPaths {

    private static final Path BASE = resolveBase();

    private AppPaths() {}

    private static Path resolveBase() {
        String override = System.getProperty("unsubscribeos.home");
        if (override != null) return Path.of(override);
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path root = xdg != null && !xdg.isBlank()
                ? Path.of(xdg)
                : Path.of(System.getProperty("user.home"), ".config");
        return root.resolve("unsubscribeos");
    }

    public static Path dir() {
        ensureDir();
        return BASE;
    }

    public static Path tokens(String provider)      { return dir().resolve("tokens-" + provider.toLowerCase() + ".enc"); }
    public static Path credentials(String provider) { return dir().resolve("creds-" + provider.toLowerCase() + ".enc"); }
    public static Path keyFile()                     { return dir().resolve("master.key"); }
    public static Path settings()                    { return dir().resolve("settings.properties"); }

    private static void ensureDir() {
        try {
            if (Files.isDirectory(BASE)) return;
            Files.createDirectories(BASE);
            trySetOwnerOnly(BASE);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create app directory: " + BASE, e);
        }
    }

    /** Best-effort 0700/0600 lockdown; silently skipped on non-POSIX filesystems. */
    public static void trySetOwnerOnly(Path path) {
        try {
            boolean isDir = Files.isDirectory(path);
            Files.setPosixFilePermissions(path, isDir
                    ? Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
                    : Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX (e.g. Windows) — directory ACLs already default to the user.
        }
    }
}
