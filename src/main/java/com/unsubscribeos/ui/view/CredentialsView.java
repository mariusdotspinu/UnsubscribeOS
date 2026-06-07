package com.unsubscribeos.ui.view;

import com.unsubscribeos.config.ProviderConfigs;
import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Async;
import com.unsubscribeos.ui.Errors;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.component.Tutorial;
import com.unsubscribeos.ui.component.Ui;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Second screen: a collapsible guide on creating the provider's OAuth client, the credential
 * fields, and the sign-in action. Sign-in runs off the UI thread and lands on the dashboard.
 */
public final class CredentialsView {

    private static final double FORM_WIDTH = 560;

    private final AppContext context;
    private final Router router;
    private final Provider provider;
    private final boolean secretRequired;

    private final TextField clientId = new TextField();
    private final PasswordField clientSecret = new PasswordField();
    private final CheckBox remember = new CheckBox("Remember me");
    private final Label status = Ui.label("", "muted");
    private final Button signIn = Ui.button("", () -> {}, "primary");

    public CredentialsView(AppContext context, Router router, Provider provider) {
        this.context = context;
        this.router = router;
        this.provider = provider;
        this.secretRequired = ProviderConfigs.requiresSecret(provider);
    }

    public Parent build() {
        clientId.setPromptText("Client ID");
        clientSecret.setPromptText("Client secret");
        signIn.setText("Connect " + provider.displayName());
        signIn.setOnAction(e -> signIn());
        signIn.disableProperty().bind(invalid());
        prefillRemembered();

        VBox form = new VBox(14,
                header(),
                help(),
                field("Client ID", clientId),
                secretRequired ? field("Client secret", clientSecret) : new VBox(),
                remember,
                signIn,
                status);
        form.setMaxWidth(FORM_WIDTH);
        form.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane(centered(form));
        root.getStyleClass().add("root");
        root.setTop(topBar());
        root.setPadding(new Insets(20));
        return root;
    }

    private HBox header() {
        return Ui.row(12, Pos.CENTER_LEFT,
                ProviderLogo.of(provider, 30),
                Ui.label("Connect your " + provider.displayName(), "section"));
    }

    private TitledPane help() {
        TitledPane pane = new TitledPane("How do I get my credentials?",
                Tutorial.of(ProviderConfigs.helpText(provider)));
        pane.setExpanded(false);
        return pane;
    }

    private VBox field(String label, TextField input) {
        return new VBox(6, Ui.label(label, "muted"), input);
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

    private HBox topBar() {
        return Ui.row(10, Pos.CENTER_LEFT,
                Ui.button("←  Back", router::toProvider, "ghost"),
                Ui.grow(),
                ThemeToggle.button(context.themeManager()));
    }

    private VBox centered(VBox form) {
        VBox box = new VBox(form);
        box.setAlignment(Pos.CENTER);
        return box;
    }

}
