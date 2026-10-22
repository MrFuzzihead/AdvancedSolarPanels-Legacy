package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.SolarMath;

class SolarPanelGenerationTest {

    private static TileEntitySolarPanel panel(int day, int output, int storage) {
        TileEntitySolarPanel panel = new TileEntitySolarPanel("test", 0, day, output, storage);
        // Avoid the 128-tick visibility refresh: this test does not have a World.
        panel.ticker = 1;
        return panel;
    }

    @Test
    void fullSunUsesFullGeneration() {
        TileEntitySolarPanel panel = panel(8, 32, 32_000);
        panel.generationFactor = 1.0D;
        assertEquals(8, panel.gainFuel());
        assertEquals(8, panel.generating);
        assertTrue(panel.active);
    }

    @Test
    void duskUsesQuarterGeneration() {
        TileEntitySolarPanel panel = panel(8, 32, 32_000);
        panel.generationFactor = SolarMath.DAWN_DUSK_FACTOR;
        assertEquals(2, panel.gainFuel());
    }

    @Test
    void nightOrRainUsesEighthGeneration() {
        TileEntitySolarPanel panel = panel(8, 32, 32_000);
        panel.generationFactor = SolarMath.NIGHT_FACTOR;
        assertEquals(1, panel.gainFuel());
    }

    @Test
    void blockedSkyProducesNothingAndClearsActive() {
        TileEntitySolarPanel panel = panel(8, 32, 32_000);
        panel.active = true;
        panel.generationFactor = 0.0D;
        assertEquals(0, panel.gainFuel());
        assertEquals(0, panel.generating);
        assertFalse(panel.active);
    }

    @Test
    void fractionalGenerationIsAccumulated() {
        TileEntitySolarPanel panel = panel(10, 32, 32_000);
        panel.generationFactor = 0.25D;
        assertEquals(2, panel.gainFuel());
        assertEquals(3, panel.gainFuel());
    }

    @Test
    void gaugeHandlesZeroAndOversizedStorage() {
        TileEntitySolarPanel panel = panel(8, 32, 100);
        panel.storage = 50;
        assertEquals(12, panel.gaugeEnergyScaled(24));

        panel.storage = 500;
        assertEquals(24, panel.gaugeEnergyScaled(24));

        panel.maxStorage = 0;
        assertEquals(0, panel.gaugeEnergyScaled(24));
    }

    @Test
    void drawEnergyNeverGoesNegative() {
        TileEntitySolarPanel panel = panel(8, 32, 100);
        panel.storage = 10;
        panel.drawEnergy(100);
        assertEquals(0, panel.storage);
    }

    @Test
    void offeredEnergyIsClamped() {
        TileEntitySolarPanel panel = panel(8, 32, 100);
        panel.storage = 10;
        assertEquals(10.0D, panel.getOfferedEnergy(), 0.0D);

        panel.storage = 0;
        assertEquals(0.0D, panel.getOfferedEnergy(), 0.0D);
    }

    @Test
    void wrenchDropRateMatchesIc2Solar() {
        TileEntitySolarPanel panel = panel(8, 32, 32_000);
        assertEquals(0.9F, panel.getWrenchDropRate(), 0.0F);
    }

    @Test
    void nbtStorageIsClampedToCapacity() {
        TileEntitySolarPanel panel = panel(8, 32, 100);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", 99_999);
        panel.readFromNBT(tag);
        assertEquals(100, panel.storage);
    }
}
