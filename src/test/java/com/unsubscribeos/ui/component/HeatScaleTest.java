package com.unsubscribeos.ui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatScaleTest {

    @Test
    void busiestDomainIsHottest() {
        assertEquals("heat-0", HeatScale.classFor(100, 100));
    }

    @Test
    void quietestDomainIsCoolest() {
        assertEquals("heat-5", HeatScale.classFor(1, 100));
    }

    @Test
    void gradesByRatioToBusiest() {
        assertEquals("heat-1", HeatScale.classFor(60, 100));  // 0.60 >= 0.55
        assertEquals("heat-2", HeatScale.classFor(40, 100));  // 0.40 >= 0.35
        assertEquals("heat-4", HeatScale.classFor(10, 100));  // 0.10 >= 0.08
    }

    @Test
    void emptyScaleIsCoolest() {
        assertEquals("heat-5", HeatScale.classFor(0, 0));
    }
}
