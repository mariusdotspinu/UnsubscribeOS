package com.unsubscribeos.core.platform;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/** Opens URLs (and mailto: links) in the user's default application, cross-platform incl. WSL. */
public final class Browser {

    private Browser() {}

    public static void open(String uri) {
        if (tryDesktop(uri)) return;
        launchViaShell(uri);
    }

    private static boolean tryDesktop(String uri) {
        try {
            boolean mail = uri.regionMatches(true, 0, "mailto:", 0, 7);
            Desktop.Action action = mail ? Desktop.Action.MAIL : Desktop.Action.BROWSE;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)) {
                if (mail) Desktop.getDesktop().mail(URI.create(uri));
                else Desktop.getDesktop().browse(URI.create(uri));
                return true;
            }
        } catch (Exception ignored) {
            // fall through to an OS launcher
        }
        return false;
    }

    private static void launchViaShell(String uri) {
        for (String[] command : launchers(uri)) {
            if (run(command)) return;
        }
        throw new IllegalStateException("No browser launcher available for: " + uri);
    }

    /** Candidate launchers in priority order; the first one present on the system wins. */
    private static List<String[]> launchers(String uri) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return List.<String[]>of(new String[]{"rundll32", "url.dll,FileProtocolHandler", uri});
        }
        if (os.contains("mac")) {
            return List.<String[]>of(new String[]{"open", uri});
        }
        // Linux and WSL: native openers first, then Windows interop so WSL still opens a browser.
        return List.of(
                new String[]{"xdg-open", uri},
                new String[]{"gio", "open", uri},
                new String[]{"wslview", uri},
                new String[]{"explorer.exe", uri},
                new String[]{"powershell.exe", "-NoProfile", "Start-Process", uri});
    }

    private static boolean run(String[] command) {
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException e) {
            return false; // launcher not installed — try the next candidate
        }
    }
}
