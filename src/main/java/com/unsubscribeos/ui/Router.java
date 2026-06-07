package com.unsubscribeos.ui;

import com.unsubscribeos.core.model.Account;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.view.CredentialsView;
import com.unsubscribeos.ui.view.ProviderView;
import com.unsubscribeos.ui.view.dashboard.DashboardView;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Single navigation point between the app's screens. Builds each view on demand with the
 * shared {@link AppContext}, so views never reference one another directly.
 */
public final class Router {

    private final Scene scene;
    private final AppContext context;

    public Router(Scene scene, AppContext context) {
        this.scene = scene;
        this.context = context;
    }

    public void toProvider() {
        show(new ProviderView(context, this).build());
    }

    public void toCredentials(Provider provider) {
        show(new CredentialsView(context, this, provider).build());
    }

    public void toDashboard(Account account) {
        show(new DashboardView(context, this, account).build());
    }

    private void show(Parent view) {
        scene.setRoot(view);
    }
}
