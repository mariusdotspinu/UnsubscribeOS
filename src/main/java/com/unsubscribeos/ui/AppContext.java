package com.unsubscribeos.ui;

import com.unsubscribeos.core.auth.AuthService;
import com.unsubscribeos.core.unsubscribe.UnsubscribeService;

/** Immutable container of shared services handed to views — lightweight dependency injection. */
public record AppContext(
        AuthService authService,
        UnsubscribeService unsubscribeService,
        ThemeManager themeManager) {
}
