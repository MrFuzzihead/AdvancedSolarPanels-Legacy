package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;

class MolecularTransformerLogicTest {

    private static ItemStack stack() {
        return new ItemStack(new Item(), 1, 0);
    }

    @Test
    void demandAccountsForBufferedEnergy() {
        TileEntityMolecularTransformer transformer = new TileEntityMolecularTransformer();
        transformer.doWork = true;
        transformer.lastRecipeEnergyPerOperation = 100;
        transformer.lastRecipeEnergyUsed = 0.0D;
        transformer.energyBuffer = 0.0D;

        assertEquals(100.0D, transformer.getDemandedEnergy(), 0.0D);
        assertEquals(0.0D, transformer.injectEnergy(null, 40.0D, 1.0D), 0.0D);
        assertEquals(40.0D, transformer.energyBuffer, 0.0D);
        assertEquals(60.0D, transformer.getDemandedEnergy(), 0.0D);
    }

    @Test
    void blockedOutputRejectsEnergy() {
        TileEntityMolecularTransformer transformer = new TileEntityMolecularTransformer();
        transformer.doWork = true;
        transformer.waitOutputSlot = true;
        transformer.lastRecipeEnergyPerOperation = 100;

        assertEquals(0.0D, transformer.getDemandedEnergy(), 0.0D);
        assertEquals(25.0D, transformer.injectEnergy(null, 25.0D, 1.0D), 0.0D);
        assertEquals(0.0D, transformer.energyBuffer, 0.0D);
    }

    @Test
    void completesRecipeAndReturnsLeftoverEnergy() {
        TileEntityMolecularTransformer transformer = new TileEntityMolecularTransformer();
        transformer.doWork = true;
        transformer.lastRecipeEnergyPerOperation = 100;
        transformer.lastRecipeEnergyUsed = 0.0D;
        transformer.energyBuffer = 0.0D;
        transformer.lastRecipeOutput = stack();

        assertEquals(0.0D, transformer.injectEnergy(null, 40.0D, 1.0D), 0.0D);
        transformer.energyBuffer = transformer.gainFuel(transformer.energyBuffer);
        assertEquals(40.0D, transformer.lastRecipeEnergyUsed, 0.0D);
        assertEquals(60.0D, transformer.getDemandedEnergy(), 0.0D);

        assertEquals(0.0D, transformer.injectEnergy(null, 60.0D, 1.0D), 0.0D);
        transformer.energyBuffer = transformer.gainFuel(transformer.energyBuffer);
        assertFalse(transformer.doWork);
        assertFalse(transformer.waitOutputSlot);
        assertNotNull(transformer.getStackInSlot(1));
    }

    @Test
    void negativeEnergyIsIgnored() {
        TileEntityMolecularTransformer transformer = new TileEntityMolecularTransformer();
        transformer.doWork = true;
        transformer.lastRecipeEnergyPerOperation = 100;
        transformer.energyBuffer = 0.0D;

        assertEquals(0.0D, transformer.gainFuel(-10.0D), 0.0D);
        assertEquals(0.0D, transformer.lastRecipeEnergyUsed, 0.0D);
    }

    @Test
    void readsEnergyBufferAndWaitStateFromNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("energyBuffer", 12.5D);
        tag.setBoolean("waitOutputSlot", true);

        TileEntityMolecularTransformer copy = new TileEntityMolecularTransformer();
        copy.readFromNBT(tag);

        assertEquals(12.5D, copy.energyBuffer, 0.0D);
        assertTrue(copy.waitOutputSlot);
    }

    @Test
    void inputSlotCannotBeExtractedAndOutputCanBe() {
        TileEntityMolecularTransformer transformer = new TileEntityMolecularTransformer();
        assertEquals(1, transformer.getAccessibleSlotsFromSide(0).length);
        assertEquals(1, transformer.getAccessibleSlotsFromSide(0)[0]);
        assertFalse(transformer.canExtractItem(0, stack(), 0));
        assertTrue(transformer.canExtractItem(1, stack(), 0));
    }
}
