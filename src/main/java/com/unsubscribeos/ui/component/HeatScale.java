package com.unsubscribeos.ui.component;

/**
 * Maps a domain's email volume to a heat CSS class relative to the busiest domain:
 * {@code heat-0} (dark red, most) through {@code heat-5} (dark green, fewest).
 */
public final class HeatScale {

    private static final double[] THRESHOLDS = {0.80, 0.55, 0.35, 0.20, 0.08};

    private HeatScale() {}

    public static String classFor(int count, int maxCount) {
        if (maxCount <= 0) return "heat-5";
        double ratio = (double) count / maxCount;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (ratio >= THRESHOLDS[i]) return "heat-" + i;
        }
        return "heat-" + THRESHOLDS.length;
    }
}
