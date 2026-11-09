package io.github.brooswitminecraft.layeredslopes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnchorTest {
    @Test
    void floorIsNotCeiling() {
        assertFalse(Anchor.FLOOR.isCeiling());
        assertEquals("floor", Anchor.FLOOR.getSerializedName());
    }

    @Test
    void ceilingIsCeiling() {
        assertTrue(Anchor.CEILING.isCeiling());
        assertEquals("ceiling", Anchor.CEILING.getSerializedName());
    }
}
