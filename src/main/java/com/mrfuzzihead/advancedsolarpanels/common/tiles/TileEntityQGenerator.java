package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.advancedsolarpanels.common.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerQGenerator;
import com.mrfuzzihead.advancedsolarpanels.network.IHasButton;

import ic2.api.Direction;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.tile.IWrenchable;

public class TileEntityQGenerator extends TileEntityBase
    implements IEnergyTile, IWrenchable, IEnergySource, IHasButton {

    public static Random randomizer = new Random();
    public int ticker;
    private int machineTire;
    public int production;
    public boolean initialized;
    private short facing;
    public boolean addedToEnergyNet;
    private boolean created;
    public boolean active;
    public boolean lastState;
    public int maxPacketSize;
    private int lastX;
    private int lastY;
    private int lastZ;
    public boolean loaded = false;
    private static List<String> fields = Arrays.asList();

    public TileEntityQGenerator() {
        this.created = false;
        this.facing = 2;
        this.initialized = false;
        this.production = AdvancedSolarPanel.qgbaseProduction;
        this.maxPacketSize = AdvancedSolarPanel.qgbaseMaxPacketSize;
        this.ticker = randomizer.nextInt(this.tickRate());
        this.lastX = this.xCoord;
        this.lastY = this.yCoord;
        this.lastZ = this.zCoord;
        this.lastState = false;
        this.machineTire = Integer.MAX_VALUE;
    }

    public void validate() {
        super.validate();
        if (!this.isInvalid() && this.worldObj.blockExists(this.xCoord, this.yCoord, this.zCoord)) {
            this.onLoaded();
        }
    }

    public void invalidate() {
        if (this.loaded) {
            this.onUnloaded();
        }

        super.invalidate();
    }

    public void onLoaded() {
        if (!this.worldObj.isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            this.addedToEnergyNet = true;
        }

        this.loaded = true;
    }

    public void onChunkUnload() {
        if (this.loaded) {
            this.onUnloaded();
        }

        super.onChunkUnload();
    }

    public void onUnloaded() {
        if (this.addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.addedToEnergyNet = false;
        }

        this.loaded = false;
    }

    public boolean canUpdate() {
        return true;
    }

    public void updateEntity() {
        super.updateEntity();
        if (!this.worldObj.isRemote) {
            if (!this.addedToEnergyNet) {
                this.onLoaded();
            }

            if (this.lastX != this.xCoord || this.lastZ != this.zCoord || this.lastY != this.yCoord) {
                this.lastX = this.xCoord;
                this.lastY = this.yCoord;
                this.lastZ = this.zCoord;
                this.onUnloaded();
                this.onLoaded();
            }
        }
    }

    @Override
    public boolean getActive() {
        this.active = this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
        if (this.active != this.lastState) {
            this.lastState = this.active;
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }

        return this.active;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.production = nbttagcompound.getInteger("production");
        this.maxPacketSize = nbttagcompound.getInteger("maxPacketSize");
        this.lastX = nbttagcompound.getInteger("lastX");
        this.lastY = nbttagcompound.getInteger("lastY");
        this.lastZ = nbttagcompound.getInteger("lastZ");
    }

    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        new NBTTagList();
        nbttagcompound.setInteger("production", this.production);
        nbttagcompound.setInteger("maxPacketSize", this.maxPacketSize);
        nbttagcompound.setInteger("lastX", this.lastX);
        nbttagcompound.setInteger("lastY", this.lastY);
        nbttagcompound.setInteger("lastZ", this.lastZ);
    }

    public boolean isAddedToEnergyNet() {
        return this.addedToEnergyNet;
    }

    public boolean emitsEnergyTo(TileEntity receiver, Direction direction) {
        return true;
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false
            : entityplayer.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64.0;
    }

    public int tickRate() {
        return 128;
    }

    @Override
    public short getFacing() {
        return this.facing;
    }

    @Override
    public void setFacing(short facing) {
        this.facing = facing;
    }

    @Override
    public boolean wrenchCanSetFacing(EntityPlayer entityplayer, int i) {
        return false;
    }

    @Override
    public boolean wrenchCanRemove(EntityPlayer entityplayer) {
        return true;
    }

    @Override
    public float getWrenchDropRate() {
        return 1.0F;
    }

    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer) {
        return new ItemStack(
            this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord),
            1,
            this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord));
    }

    @Override
    public List<String> getNetworkedFields() {
        return fields;
    }

    public Container getGuiContainer(InventoryPlayer inventoryplayer) {
        return new ContainerQGenerator(inventoryplayer, this);
    }

    public String getInvName() {
        return "QuantumGenerator";
    }

    public void changeProductionOutput(int value) {
        this.production += value;
        if (this.production < 1) {
            this.production = 1;
        }
    }

    public void changeMaxPacketSize(int value) {
        this.maxPacketSize += value;
        if (this.maxPacketSize < 1) {
            this.maxPacketSize = 1;
        }
    }

    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true;
    }

    public double getOfferedEnergy() {
        this.getActive();
        return !this.active ? this.production : 0.0;
    }

    public void drawEnergy(double amount) {}

    @Override
    public void handleButtonClick(int event) {
        switch (event) {
            case 1:
                this.changeProductionOutput(-100);
                break;
            case 2:
                this.changeProductionOutput(-10);
                break;
            case 3:
                this.changeProductionOutput(-1);
                break;
            case 4:
                this.changeProductionOutput(1);
                break;
            case 5:
                this.changeProductionOutput(10);
                break;
            case 6:
                this.changeProductionOutput(100);
                break;
            case 7:
                this.changeMaxPacketSize(-100);
                break;
            case 8:
                this.changeMaxPacketSize(-10);
                break;
            case 9:
                this.changeMaxPacketSize(-1);
                break;
            case 10:
                this.changeMaxPacketSize(1);
                break;
            case 11:
                this.changeMaxPacketSize(10);
                break;
            case 12:
                this.changeMaxPacketSize(100);
                break;
            case 101:
                this.changeProductionOutput(-1000);
                break;
            case 102:
                this.changeProductionOutput(-100);
                break;
            case 103:
                this.changeProductionOutput(-10);
                break;
            case 104:
                this.changeProductionOutput(10);
                break;
            case 105:
                this.changeProductionOutput(100);
                break;
            case 106:
                this.changeProductionOutput(1000);
                break;
            case 107:
                this.changeMaxPacketSize(-1000);
                break;
            case 108:
                this.changeMaxPacketSize(-100);
                break;
            case 109:
                this.changeMaxPacketSize(-10);
                break;
            case 110:
                this.changeMaxPacketSize(10);
                break;
            case 111:
                this.changeMaxPacketSize(100);
                break;
            case 112:
                this.changeMaxPacketSize(1000);
        }
    }

    public int getSourceTier() {
        return this.machineTire;
    }
}
