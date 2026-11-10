package io.github.brooswitminecraft.layeredslopes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LayerMathTest {
    @Test
    void addLayerIncrementsAndClampsAtFullBlock() {
        assertEquals(2, LayerMath.addLayer(1));
        assertEquals(16, LayerMath.addLayer(15));
        assertEquals(16, LayerMath.addLayer(16));
    }

    @Test
    void canAddLayerOnlyBelowFull() {
        assertTrue(LayerMath.canAddLayer(1));
        assertTrue(LayerMath.canAddLayer(15));
        assertFalse(LayerMath.canAddLayer(16));
        assertFalse(LayerMath.canAddLayer(0));
    }

    @Test
    void floorLayersGrowUpFromTheBottom() {
        assertEquals(0, LayerMath.minY(false, 5));
        assertEquals(5, LayerMath.maxY(false, 5));
    }

    @Test
    void ceilingLayersHangDownFromTheTop() {
        assertEquals(11, LayerMath.minY(true, 5));
        assertEquals(16, LayerMath.maxY(true, 5));
    }

    @Test
    void sixteenLayersFillTheCellEitherWay() {
        for (boolean ceiling : new boolean[] {false, true}) {
            assertEquals(0, LayerMath.minY(ceiling, 16));
            assertEquals(16, LayerMath.maxY(ceiling, 16));
        }
    }

    @Test
    void walkableBelowEightLayers() {
        assertTrue(LayerMath.isWalkable(1));
        assertTrue(LayerMath.isWalkable(7));
    }

    @Test
    void notWalkableAtOrAboveEightLayers() {
        assertFalse(LayerMath.isWalkable(8));
        assertFalse(LayerMath.isWalkable(16));
    }
}
