package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.items.ItemAdvancedSolarHelmet;

class HelmetItemTest {

    @BeforeEach
    void setConfiguredOutputs() {
        AdvancedSolarPanel.advGenDay = 8;
        AdvancedSolarPanel.hGenDay = 64;
        AdvancedSolarPanel.uhGenDay = 512;
    }

    private static ItemAdvancedSolarHelmet helmet(int type) {
        return new ItemAdvancedSolarHelmet(ArmorMaterial.DIAMOND, 0, 0, type);
    }

    @Test
    void helmetStatsMatchTheirPanelTier() {
        ItemAdvancedSolarHelmet advanced = helmet(1);
        assertEquals(1, advanced.getTier(new ItemStack(advanced)));
        assertEquals(1_000_000.0D, advanced.getMaxCharge(new ItemStack(advanced)), 0.0D);
        assertEquals(3_000.0D, advanced.getTransferLimit(new ItemStack(advanced)), 0.0D);
        assertEquals(800, advanced.getEnergyPerDamage());

        ItemAdvancedSolarHelmet hybrid = helmet(2);
        assertEquals(2, hybrid.getTier(new ItemStack(hybrid)));
        assertEquals(10_000_000.0D, hybrid.getMaxCharge(new ItemStack(hybrid)), 0.0D);
        assertEquals(10_000.0D, hybrid.getTransferLimit(new ItemStack(hybrid)), 0.0D);
        assertEquals(2_000, hybrid.getEnergyPerDamage());

        ItemAdvancedSolarHelmet ultimate = helmet(3);
        assertEquals(3, ultimate.getTier(new ItemStack(ultimate)));
        assertEquals(512, AdvancedSolarPanel.uhGenDay);
    }

    @Test
    void helmetTooltipHasOutputTierAndDescription() {
        ItemAdvancedSolarHelmet hybrid = helmet(2);
        List<String> lines = new ArrayList<>();
        hybrid.addInformation(new ItemStack(hybrid), null, lines, false);

        assertFalse(lines.isEmpty());
        assertTrue(
            lines.get(0)
                .contains("64"));
        assertTrue(
            lines.get(1)
                .contains("2"));
        assertTrue(
            lines.get(2)
                .contains("chestplate"));
        assertTrue(
            lines.get(3)
                .contains("underwater air"));
    }

    @Test
    void hybridAndUltimateHaveAirSupplyButAdvancedDoesNot() {
        List<String> advanced = new ArrayList<>();
        List<String> hybrid = new ArrayList<>();
        helmet(1).addInformation(new ItemStack(helmet(1)), null, advanced, false);
        helmet(2).addInformation(new ItemStack(helmet(2)), null, hybrid, false);

        assertEquals(3, advanced.size());
        assertEquals(4, hybrid.size());
    }
}
