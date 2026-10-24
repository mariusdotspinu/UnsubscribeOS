package com.unsubscribeos.ui.view;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.ProviderLogo;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public final class ProviderView {

    private final Router router;

    @FXML private StackPane gmailLogo;
    @FXML private StackPane outlookLogo;

    public ProviderView(Router router) {
        this.router = router;
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
