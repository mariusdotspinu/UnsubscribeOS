package com.unsubscribeos.ui.view;

import com.unsubscribeos.core.model.Provider;
import com.unsubscribeos.ui.AppContext;
import com.unsubscribeos.ui.Router;
import com.unsubscribeos.ui.component.AppIcon;
import com.unsubscribeos.ui.component.ProviderLogo;
import com.unsubscribeos.ui.component.ThemeToggle;
import com.unsubscribeos.ui.component.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** First screen: pick the mail provider to connect. The single entry into the sign-in flow. */
public final class ProviderView {

    private final AppContext context;
    private final Router router;

    public ProviderView(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    public Parent build() {
        VBox center = new VBox(20,
                AppIcon.node(72),
                Ui.label("UnsubscribeOS", "title"),
                Ui.label("Choose your mail provider to begin.", "subtitle"),
                tiles());
        center.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane(center);
        root.getStyleClass().add("root");
        root.setTop(topBar());
        root.setPadding(new Insets(20));
        return root;
    }

    private HBox tiles() {
        return Ui.row(20, Pos.CENTER, tile(Provider.GMAIL), tile(Provider.OUTLOOK));
    }

    private VBox tile(Provider provider) {
        VBox tile = new VBox(12,
                ProviderLogo.of(provider, 56),
                Ui.label(provider.displayName(), "provider-name"));
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("provider-tile");
        tile.setOnMouseClicked(e -> router.toCredentials(provider));
        return tile;
    }

    private HBox topBar() {
        return Ui.row(10, Pos.CENTER_RIGHT, ThemeToggle.button(context.themeManager()));
    }
}
