package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityAdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityHybridSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQuantumSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityUltimateSolarPanel;

class SolarTierTest {

    @Test
    void panelTiersMatchTheirPacketSizes() {
        assertEquals(1, new TileEntityAdvancedSolarPanel().getSourceTier());
        assertEquals(2, new TileEntityHybridSolarPanel().getSourceTier());
        assertEquals(3, new TileEntityUltimateSolarPanel().getSourceTier());
        assertEquals(5, new TileEntityQuantumSolarPanel().getSourceTier());
    }

    @Test
    void chargeTierMatchesSourceTier() {
        assertEquals(1, new TileEntityAdvancedSolarPanel().getChargeTier());
        assertEquals(2, new TileEntityHybridSolarPanel().getChargeTier());
        assertEquals(3, new TileEntityUltimateSolarPanel().getChargeTier());
        assertEquals(5, new TileEntityQuantumSolarPanel().getChargeTier());
    }
}
