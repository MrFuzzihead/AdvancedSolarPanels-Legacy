package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;

class SolarPanelGenerationTest {

    private static TileEntitySolarPanel panel(int day, int night, int output, int storage) {
        TileEntitySolarPanel panel = new TileEntitySolarPanel("test", 0, day, night, output, storage);
        // Avoid the 128-tick visibility refresh: this test does not have a World.
        panel.ticker = 1;
        return panel;
    }

    @Test
    void fullSunUsesDayGeneration() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 32_000);
        panel.sunIsUp = true;
        panel.skyIsVisible = true;
        assertEquals(8, panel.gainFuel());
        assertEquals(8, panel.generating);
    }

    @Test
    void nightUsesNightGenerationWhenSkyIsVisible() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 32_000);
        panel.sunIsUp = false;
        panel.skyIsVisible = true;
        assertEquals(1, panel.gainFuel());
        assertEquals(1, panel.generating);
    }

    @Test
    void blockedSkyProducesNothing() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 32_000);
        panel.sunIsUp = true;
        panel.skyIsVisible = false;
        assertEquals(0, panel.gainFuel());
        assertEquals(0, panel.generating);
    }

    @Test
    void gaugeHandlesZeroAndOversizedStorage() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 100);
        panel.storage = 50;
        assertEquals(12, panel.gaugeEnergyScaled(24));

        panel.storage = 500;
        assertEquals(24, panel.gaugeEnergyScaled(24));

        panel.maxStorage = 0;
        assertEquals(0, panel.gaugeEnergyScaled(24));
    }

    @Test
    void drawEnergyNeverGoesNegative() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 100);
        panel.storage = 10;
        panel.drawEnergy(100);
        assertEquals(0, panel.storage);
    }

    @Test
    void offeredEnergyIsClamped() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 100);
        panel.storage = 10;
        assertEquals(10.0D, panel.getOfferedEnergy(), 0.0D);

        panel.storage = 0;
        assertEquals(0.0D, panel.getOfferedEnergy(), 0.0D);
    }

    @Test
    void wrenchDropRateMatchesIc2Solar() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 32_000);
        assertEquals(0.9F, panel.getWrenchDropRate(), 0.0F);
    }

    @Test
    void nbtStorageIsClampedToCapacity() {
        TileEntitySolarPanel panel = panel(8, 1, 32, 100);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", 99_999);
        panel.readFromNBT(tag);
        assertEquals(100, panel.storage);
    }
}
