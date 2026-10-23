package com.unsubscribeos.ui.view;

import com.unsubscribeos.config.ProviderConfigs;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.Fxml;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.component.Tutorial;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for {@code credentials.fxml}, the second screen: a collapsible guide on creating the
 * provider's OAuth client, the credential fields, and the sign-in action. Sign-in runs off the UI
 * thread and lands on the dashboard.
 */
public final class CredentialsView {

    private final AppContext context;
    private final Router router;
    private final Provider provider;
    private final boolean secretRequired;

    @FXML private StackPane logo;
    @FXML private Label heading;
    @FXML private Tutorial helpController;
    @FXML private TextField clientId;
    @FXML private VBox secretField;
    @FXML private PasswordField clientSecret;
    @FXML private CheckBox remember;
    @FXML private Button signIn;
    @FXML private Label status;

    public CredentialsView(AppContext context, Router router, Provider provider) {
        this.context = context;
        this.router = router;
        this.provider = provider;
        this.secretRequired = ProviderConfigs.requiresSecret(provider);
    }

    public Parent build() {
        return Fxml.load("credentials.fxml", this, new ThemeToggle(context.themeManager()));
    }

    @FXML
    private void initialize() {
        logo.getChildren().setAll(ProviderLogo.of(provider, 30));
        heading.setText("Connect your " + provider.displayName());
        helpController.setText(ProviderConfigs.helpText(provider));
        secretField.setVisible(secretRequired);
        secretField.setManaged(secretRequired);
        signIn.setText("Connect " + provider.displayName());
        signIn.disableProperty().bind(invalid());
        prefillRemembered();
    }

    private BooleanBinding invalid() {
        BooleanBinding empty = Bindings.isEmpty(clientId.textProperty());
        return secretRequired ? empty.or(Bindings.isEmpty(clientSecret.textProperty())) : empty;
    }

    private void prefillRemembered() {
        context.authService().rememberedCredentials(provider).ifPresent(saved -> {
            clientId.setText(saved.clientId());
            if (saved.clientSecret() != null) clientSecret.setText(saved.clientSecret());
            remember.setSelected(true);
        });
    }

    @FXML
    private void signIn() {
        signIn.disableProperty().unbind();
        signIn.setDisable(true);
        status.setText("Opening your browser to sign in…");
        String id = clientId.getText().trim();
        String secret = clientSecret.getText().trim();
        boolean keep = remember.isSelected();
        Async.supply(() -> context.authService().signIn(provider, id, secret, keep),
                router::toDashboard,
                error -> {
                    signIn.disableProperty().bind(invalid());
                    status.setText("Sign-in failed: " + Errors.describe(error));
                });
    }

    @FXML
    private void back() {
        router.toProvider();
    }
}
