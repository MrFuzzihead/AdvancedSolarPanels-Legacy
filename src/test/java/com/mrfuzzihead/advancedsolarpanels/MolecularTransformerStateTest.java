package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;

class MolecularTransformerStateTest {

    static {
        TileEntity.addMapping(TileEntityMolecularTransformer.class, "asp_molecular_transformer_test");
    }

    @BeforeEach
    void setUpRecipe() {
        MTRecipeManager.instance.clearMTRecipes();
    }

    @Test
    void nbtRoundTripPreservesSafeDisplayState() {
        TileEntityMolecularTransformer source = new TileEntityMolecularTransformer();
        source.lastRecipeEnergyPerOperation = 100;
        source.lastRecipeEnergyUsed = 40.0D;
        source.lastProgress = 40;
        source.energyBuffer = 12.5D;
        source.doWork = true;
        source.waitOutputSlot = true;
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        assertTrue(tag.hasKey("energyBuffer"));
        assertTrue(tag.hasKey("waitOutputSlot"));

        TileEntityMolecularTransformer target = new TileEntityMolecularTransformer();
        target.readFromNBT(tag);

        assertEquals(12.5D, target.energyBuffer, 0.0001D);
        assertTrue(target.waitOutputSlot);
        assertFalse(target.doWork);
        assertNull(target.lastRecipeInput);
        assertNull(target.lastRecipeOutput);
    }

    @Test
    void descriptionPacketRoundTripsDisplayState() {
        TileEntityMolecularTransformer source = new TileEntityMolecularTransformer();
        source.lastRecipeEnergyPerOperation = 12345;

        S35PacketUpdateTileEntity packet = (S35PacketUpdateTileEntity) source.getDescriptionPacket();
        TileEntityMolecularTransformer target = new TileEntityMolecularTransformer();
        target.onDataPacket(null, packet);

        assertEquals(12345, target.lastRecipeEnergyPerOperation);
        assertFalse(
            packet.func_148857_g()
                .getBoolean("active"));
    }
}
