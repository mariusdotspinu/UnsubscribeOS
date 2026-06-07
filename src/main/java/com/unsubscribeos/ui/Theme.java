package com.unsubscribeos.ui;

/** The two visual themes, each backed by a palette stylesheet and a toggle glyph. */
public enum Theme {
    DARK("/css/theme-dark.css", "☀", "Switch to light theme"),
    LIGHT("/css/theme-light.css", "☾", "Switch to dark theme");

    private final String stylesheet;
    private final String toggleGlyph;
    private final String toggleTooltip;

    Theme(String stylesheet, String toggleGlyph, String toggleTooltip) {
        this.stylesheet = stylesheet;
        this.toggleGlyph = toggleGlyph;
        this.toggleTooltip = toggleTooltip;
    }

    public String stylesheet()    { return stylesheet; }
    public String toggleGlyph()   { return toggleGlyph; }
    public String toggleTooltip() { return toggleTooltip; }

    public Theme other() {
        return this == DARK ? LIGHT : DARK;
    }
}
