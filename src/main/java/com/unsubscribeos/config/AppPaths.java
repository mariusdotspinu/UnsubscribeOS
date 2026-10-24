package com.unsubscribeos.config;

import com.unsubscribeos.core.model.Provider;

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

    public static Path tokens(Provider provider)      { return file("tokens-" + provider.name().toLowerCase() + ".enc"); }
    public static Path credentials(Provider provider) { return file("creds-" + provider.name().toLowerCase() + ".enc"); }
    public static Path keyFile()                       { return file("master.key"); }
    public static Path settings()                      { return file("settings.properties"); }

    /** {@code name} inside the app directory, creating the directory owner-only on first use. */
    private static Path file(String name) {
        try {
            if (!Files.isDirectory(BASE)) {
                Files.createDirectories(BASE);
                trySetOwnerOnly(BASE);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create app directory: " + BASE, e);
        }
        return BASE.resolve(name);
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
