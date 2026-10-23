package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
    void molecularTransformerMovesPlayerStackIntoInput() {
        ContainerMolecularTransformer container = new ContainerMolecularTransformer(
            new InventoryPlayer(null),
            new TileEntityMolecularTransformer());
        container.inventorySlots.get(2)
            .putStack(new ItemStack(new Item(), 5));

        ItemStack moved = container.transferStackInSlot(null, 2);

        assertNotNull(moved);
        assertNull(
            container.inventorySlots.get(2)
                .getStack());
        assertEquals(
            5,
            container.inventorySlots.get(0)
                .getStack().stackSize);
    }

    @Test
    void molecularTransformerMovesOutputIntoPlayerInventory() {
        ContainerMolecularTransformer container = new ContainerMolecularTransformer(
            new InventoryPlayer(null),
            new TileEntityMolecularTransformer());
        container.inventorySlots.get(1)
            .putStack(new ItemStack(new Item(), 2));

        ItemStack moved = container.transferStackInSlot(null, 1);

        assertNotNull(moved);
        assertNull(
            container.inventorySlots.get(1)
                .getStack());
        boolean found = false;
        for (int i = 2; i < container.inventorySlots.size(); i++) {
            ItemStack stack = container.inventorySlots.get(i)
                .getStack();
            if (stack != null) {
                assertEquals(2, stack.stackSize);
                found = true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found);
    }

    @Test
    void qGeneratorMovesMainInventoryToHotbar() {
        ContainerQGenerator container = new ContainerQGenerator(new InventoryPlayer(null), new TileEntityQGenerator());
        container.inventorySlots.get(0)
            .putStack(new ItemStack(new Item(), 3));

        ItemStack moved = container.transferStackInSlot(null, 0);

        assertNotNull(moved);
        assertNull(
            container.inventorySlots.get(0)
                .getStack());
        assertEquals(
            3,
            container.inventorySlots.get(27)
                .getStack().stackSize);
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
