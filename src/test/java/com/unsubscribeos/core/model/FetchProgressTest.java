package com.unsubscribeos.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FetchProgressTest {

    @Test
    void ratioIsFetchedOverTotal() {
        assertEquals(0.5, new FetchProgress(50, 100).ratio());
    }

    @Test
    void ratioIsZeroWhenTotalUnknown() {
        assertEquals(0.0, new FetchProgress(0, 0).ratio());
    }
}
