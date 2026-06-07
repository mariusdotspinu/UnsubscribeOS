package com.unsubscribeos;

import com.unsubscribeos.config.Settings;
import com.unsubscribeos.core.auth.AuthService;
import com.unsubscribeos.core.auth.EncryptedAccountStore;
import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.core.unsubscribe.UnsubscribeService;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.ThemeManager;
import com.unsubscribeos.ui.component.AppIcon;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Application entry point. Wires the service graph, opens the window on the provider screen,
 * then asynchronously restores any saved account so a returning user lands straight on their
 * dashboard — staying signed in until they explicitly sign out.
 */
public final class Launcher extends Application {

    private static final double WIDTH = 1024;
    private static final double HEIGHT = 720;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Settings settings = new Settings();
        AuthService authService = new AuthService(new EncryptedAccountStore());
        AppContext context = new AppContext(authService, new UnsubscribeService(), new ThemeManager(settings));

        Scene scene = new Scene(new Region(), WIDTH, HEIGHT);
        context.themeManager().attach(scene);
        Router router = new Router(scene, context);
        router.toProvider();

        Async.supply(() -> restoreSession(authService),
                session -> session.ifPresent(router::toDashboard),
                error -> { /* no/expired session — stay on the provider screen */ });

        stage.setTitle("UnsubscribeOS");
        stage.getIcons().setAll(AppIcon.icons());
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(560);
        stage.show();
    }

    /** First provider with a persisted, refreshable account — the user's last session. */
    private Optional<Account> restoreSession(AuthService authService) {
        return Arrays.stream(Provider.values())
                .map(authService::restore)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
