package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;

class QGeneratorTest {

    static {
        TileEntity.addMapping(TileEntityQGenerator.class, "asp_qgenerator_test");
    }

    @Test
    void productionAndPacketSizeCannotDropBelowOne() {
        TileEntityQGenerator generator = new TileEntityQGenerator();
        generator.production = 1;
        generator.maxPacketSize = 1;

        generator.changeProductionOutput(-100);
        generator.changeMaxPacketSize(-100);

        assertEquals(1, generator.production);
        assertEquals(1, generator.maxPacketSize);
    }

    @Test
    void productionAndPacketSizeCannotOverflow() {
        TileEntityQGenerator generator = new TileEntityQGenerator();
        generator.production = Integer.MAX_VALUE;
        generator.maxPacketSize = Integer.MAX_VALUE;

        generator.changeProductionOutput(100);
        generator.changeMaxPacketSize(100);

        assertEquals(Integer.MAX_VALUE, generator.production);
        assertEquals(Integer.MAX_VALUE, generator.maxPacketSize);
    }

    @Test
    void buttonsAdjustProductionAndPacketSizeIncludingShiftSteps() {
        TileEntityQGenerator generator = new TileEntityQGenerator();
        generator.production = 5000;
        generator.maxPacketSize = 5000;

        generator.handleButtonClick(1);
        assertEquals(4900, generator.production);
        generator.handleButtonClick(6);
        assertEquals(5000, generator.production);
        generator.handleButtonClick(7);
        assertEquals(4900, generator.maxPacketSize);
        generator.handleButtonClick(12);
        assertEquals(5000, generator.maxPacketSize);
        generator.handleButtonClick(101);
        assertEquals(4000, generator.production);
        generator.handleButtonClick(107);
        assertEquals(4000, generator.maxPacketSize);
        generator.handleButtonClick(112);
        assertEquals(5000, generator.maxPacketSize);
    }

    @Test
    void nbtRoundTripKeepsConfigurationAndClampsInvalidValues() {
        TileEntityQGenerator source = new TileEntityQGenerator();
        source.production = 1234;
        source.maxPacketSize = 5678;
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        TileEntityQGenerator target = new TileEntityQGenerator();
        target.readFromNBT(tag);
        assertEquals(1234, target.production);
        assertEquals(5678, target.maxPacketSize);

        tag.setInteger("production", -10);
        tag.setInteger("maxPacketSize", 0);
        target.readFromNBT(tag);
        assertEquals(1, target.production);
        assertEquals(1, target.maxPacketSize);
    }
}
