package com.unsubscribeos;

/**
 * Plain (non-{@link javafx.application.Application}) entry point. Launching a fat jar whose main
 * class extends Application fails with "JavaFX runtime components are missing", because the JavaFX
 * bootstrap requires its modules on the module path in that case. Delegating from a non-Application
 * main sidesteps that check, so {@code java -jar} works with JavaFX bundled on the classpath.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Launcher.main(args);
    }
}
