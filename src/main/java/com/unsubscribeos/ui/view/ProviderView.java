package com.unsubscribeos.ui.view;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Fxml;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.ThemeToggle;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/**
 * Controller for {@code provider.fxml}, the first screen: pick the mail provider to connect.
 * The single entry into the sign-in flow.
 */
public final class ProviderView {

    private final AppContext context;
    private final Router router;

    @FXML private StackPane gmailLogo;
    @FXML private StackPane outlookLogo;

    public ProviderView(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    public Parent build() {
        return Fxml.load("provider.fxml", this, new ThemeToggle(context.themeManager()));
    }

    @FXML
    private void initialize() {
        gmailLogo.getChildren().setAll(ProviderLogo.of(Provider.GMAIL, 56));
        outlookLogo.getChildren().setAll(ProviderLogo.of(Provider.OUTLOOK, 56));
    }

    @FXML
    private void chooseGmail() {
        router.toCredentials(Provider.GMAIL);
    }

    @FXML
    private void chooseOutlook() {
        router.toCredentials(Provider.OUTLOOK);
    }
}
