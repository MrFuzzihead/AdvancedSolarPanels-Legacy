package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.entity.player.InventoryPlayer;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerQGenerator;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;

class ContainerSlotTest {

    @Test
    void molecularTransformerHasTwoMachineSlotsAndPlayerInventory() {
        ContainerMolecularTransformer container = new ContainerMolecularTransformer(
            new InventoryPlayer(null),
            new TileEntityMolecularTransformer());

        assertEquals(38, container.inventorySlots.size());
        assertEquals(
            0,
            container.inventorySlots.get(0)
                .getSlotIndex());
        assertEquals(
            1,
            container.inventorySlots.get(1)
                .getSlotIndex());
        assertEquals(
            9,
            container.inventorySlots.get(2)
                .getSlotIndex());
        assertEquals(
            8,
            container.inventorySlots.get(37)
                .getSlotIndex());
        assertNull(container.transferStackInSlot(null, 0));
    }

    @Test
    void qGeneratorHasOnlyPlayerInventory() {
        ContainerQGenerator container = new ContainerQGenerator(new InventoryPlayer(null), new TileEntityQGenerator());

        assertEquals(36, container.inventorySlots.size());
        assertEquals(
            9,
            container.inventorySlots.get(0)
                .getSlotIndex());
        assertEquals(
            0,
            container.inventorySlots.get(27)
                .getSlotIndex());
        assertNull(container.transferStackInSlot(null, 0));
    }
}
