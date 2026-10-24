package com.unsubscribeos.ui.component;

import com.unsubscribeos.ui.Theme;
import com.unsubscribeos.ui.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * Controller for {@code theme-toggle.fxml}, the theme switch shared by every screen; its glyph
 * follows the active theme. Supply an instance to the view's loader so the include picks it up.
 */
public final class ThemeToggle {

    private final ThemeManager themeManager;

    @FXML private Button button;
    @FXML private Tooltip tooltip;

    public ThemeToggle(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    @FXML
    private void initialize() {
        show(themeManager.themeProperty().get());
        themeManager.themeProperty().addListener((obs, was, now) -> show(now));
    }

    @FXML
    private void toggle() {
        themeManager.toggle();
    }

    private void show(Theme theme) {
        button.setText(theme.toggleGlyph());
        tooltip.setText(theme.toggleTooltip());
    }
}
