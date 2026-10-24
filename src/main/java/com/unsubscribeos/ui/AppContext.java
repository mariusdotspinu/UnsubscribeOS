package com.unsubscribeos.ui;

import com.unsubscribeos.config.Settings;
import com.unsubscribeos.core.auth.AuthService;
import com.unsubscribeos.core.unsubscribe.UnsubscribeService;

public record AppContext(
        AuthService authService,
        UnsubscribeService unsubscribeService,
        ThemeManager themeManager,
        Settings settings) {
}
