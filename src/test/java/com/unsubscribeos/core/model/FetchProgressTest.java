package com.unsubscribeos.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FetchProgressTest {

    @Test
    void ratioIsFetchedOverTotal() {
        assertEquals(0.5, new FetchProgress(50, 100).ratio());
    }

    @Test
    void ratioIsZeroWhenTotalUnknown() {
        assertEquals(0.0, new FetchProgress(0, 0).ratio());
    }

    @Test
    void doneOnlyWhenAllFetched() {
        assertFalse(new FetchProgress(99, 100).done());
        assertTrue(new FetchProgress(100, 100).done());
        assertFalse(new FetchProgress(0, 0).done());
    }
}
