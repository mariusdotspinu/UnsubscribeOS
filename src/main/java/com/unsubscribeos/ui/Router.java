package com.unsubscribeos.ui;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.view.CredentialsView;
import com.unsubscribeos.ui.view.ProviderView;
import com.unsubscribeos.ui.view.dashboard.DashboardView;
import javafx.scene.Scene;

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
