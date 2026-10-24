package com.unsubscribeos.ui;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.view.CredentialsView;
import com.unsubscribeos.ui.view.ProviderView;
import com.unsubscribeos.ui.view.dashboard.DashboardView;
import javafx.scene.Scene;

/**
 * Single navigation point between the app's screens. Loads each screen's FXML with its
 * controller (plus the shared theme toggle), so views never reference one another directly.
 */
public final class Router {

    private final Scene scene;
    private final AppContext context;

    public Router(Scene scene, AppContext context) {
        this.scene = scene;
        this.context = context;
    }

    public void toProvider() {
        show("provider.fxml", new ProviderView(this));
    }

    public void toCredentials(Provider provider) {
        show("credentials.fxml", new CredentialsView(context, this, provider));
    }

    public void toDashboard(Account account) {
        show("dashboard.fxml", new DashboardView(context, this, account));
    }

    private void show(String fxml, Object controller) {
        scene.setRoot(Fxml.load(fxml, controller, new ThemeToggle(context.themeManager())));
    }
}
