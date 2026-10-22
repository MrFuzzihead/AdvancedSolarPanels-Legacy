package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.blocks.BlockAdvSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.items.ItemAdvSolarPanel;

class PanelTooltipTest {

    private ItemAdvSolarPanel item;

    @BeforeEach
    void setUp() {
        AdvancedSolarPanel.advGenDay = 8;
        AdvancedSolarPanel.hGenDay = 64;
        AdvancedSolarPanel.uhGenDay = 512;
        AdvancedSolarPanel.qpGenDay = 4096;
        AdvancedSolarPanel.advStorage = 32_000;
        AdvancedSolarPanel.hStorage = 100_000;
        AdvancedSolarPanel.uhStorage = 1_000_000;
        AdvancedSolarPanel.qpStorage = 10_000_000;
        this.item = new ItemAdvSolarPanel(new BlockAdvSolarPanel());
    }

    @Test
    void panelTiersAndStorageMatchMetadata() {
        assertEquals(1, ItemAdvSolarPanel.panelTier(0));
        assertEquals(2, ItemAdvSolarPanel.panelTier(1));
        assertEquals(3, ItemAdvSolarPanel.panelTier(2));
        assertEquals(5, ItemAdvSolarPanel.panelTier(3));

        assertEquals(8, ItemAdvSolarPanel.panelDayOutput(0));
        assertEquals(64, ItemAdvSolarPanel.panelDayOutput(1));
        assertEquals(512, ItemAdvSolarPanel.panelDayOutput(2));
        assertEquals(4096, ItemAdvSolarPanel.panelDayOutput(3));

        assertEquals(32_000, ItemAdvSolarPanel.panelStorage(0));
        assertEquals(10_000_000, ItemAdvSolarPanel.panelStorage(3));
    }

    @Test
    void tooltipShowsOutputTierAndStorage() {
        List<String> lines = new ArrayList<>();
        this.item.addInformation(new ItemStack(this.item, 1, 2), null, lines, false);

        assertEquals(3, lines.size());
        assertTrue(
            lines.get(0)
                .contains("512"));
        assertTrue(
            lines.get(1)
                .contains("3"));
        assertTrue(
            lines.get(2)
                .contains("1000000"));
    }

    @Test
    void qgenHasNoPanelTooltip() {
        List<String> lines = new ArrayList<>();
        this.item.addInformation(new ItemStack(this.item, 1, 4), null, lines, false);
        assertTrue(lines.isEmpty());
    }
}
