package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;

class SolarTierTest {

    @Test
    void panelTiersMatchTheirPacketSizes() {
        assertEquals(1, panel(1).getSourceTier());
        assertEquals(2, panel(2).getSourceTier());
        assertEquals(3, panel(3).getSourceTier());
        assertEquals(5, panel(5).getSourceTier());
    }

    @Test
    void chargeTierMatchesSourceTier() {
        assertEquals(1, panel(1).getChargeTier());
        assertEquals(2, panel(2).getChargeTier());
        assertEquals(3, panel(3).getChargeTier());
        assertEquals(5, panel(5).getChargeTier());
    }

    private static TileEntitySolarPanel panel(int tier) {
        return new TileEntitySolarPanel("test", 0, 8, 1, 32, 32_000, tier);
    }
}
