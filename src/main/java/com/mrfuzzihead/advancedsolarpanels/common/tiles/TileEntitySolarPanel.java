package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerAdvSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.SolarMath;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.tile.IWrenchable;

public class TileEntitySolarPanel extends TileEntityBase
    implements IEnergyTile, IWrenchable, IEnergySource, IInventory, INetworkDataProvider, INetworkUpdateListener {

    public static Random randomizer = new Random();
    public int ticker;
    public int generating;
    public int genDay;
    public boolean initialized;
    public boolean sunIsUp;
    public boolean skyIsVisible;
    public double generationFactor;
    private double generationRemainder;
    private short facing;
    private int machineTire;
    public boolean addedToEnergyNet;
    private boolean created;
    private ItemStack[] chargeSlots;
    public int fuel;
    private int lastX;
    private int lastY;
    private int lastZ;
    public int storage;
    private int solarType;
    public String panelName;
    public int production;
    public int maxStorage;
    public boolean loaded = false;
    private static List<String> fields = Arrays.asList();

    public TileEntitySolarPanel(String gName, int typeSolar, int gDay, int gOutput, int gmaxStorage) {
        this(gName, typeSolar, gDay, gOutput, gmaxStorage, 1);
    }

    public TileEntitySolarPanel(String gName, int typeSolar, int gDay, int gOutput, int gmaxStorage, int tier) {
        this.created = false;
        this.facing = 2;
        this.solarType = typeSolar;
        this.genDay = gDay;
        this.storage = 0;
        this.panelName = gName;
        this.sunIsUp = false;
        this.skyIsVisible = false;
        this.maxStorage = gmaxStorage;
        this.chargeSlots = new ItemStack[4];
        this.initialized = false;
        this.production = gOutput;
        this.ticker = randomizer.nextInt(this.tickRate());
        this.lastX = this.xCoord;
        this.lastY = this.yCoord;
        this.lastZ = this.zCoord;
        this.machineTire = Math.max(1, tier);
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
        if (!this.worldObj.isRemote && !this.addedToEnergyNet) {
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
        if (!this.worldObj.isRemote && this.addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.addedToEnergyNet = false;
        }

        this.loaded = false;
        this.initialized = false;
    }

    public void intialize() {
        this.updateVisibility();
        this.initialized = true;
        if (!this.addedToEnergyNet) {
            this.onLoaded();
        }
    }

    public void updateEntity() {
        super.updateEntity();
        if (!this.initialized && this.worldObj != null) {
            this.intialize();
        }

        if (!this.worldObj.isRemote) {
            if (this.lastX != this.xCoord || this.lastZ != this.zCoord || this.lastY != this.yCoord) {
                this.lastX = this.xCoord;
                this.lastY = this.yCoord;
                this.lastZ = this.zCoord;
                this.onUnloaded();
                this.intialize();
            }

            this.gainFuel();
            if (this.generating > 0) {
                long newStorage = (long) this.storage + this.generating;
                this.storage = (int) Math.min((long) Math.max(1, this.maxStorage), newStorage);
            }

            boolean needInvUpdate = false;
            double sentPacket = 0.0;

            for (int i = 0; i < this.chargeSlots.length; i++) {
                if (this.chargeSlots[i] != null && this.chargeSlots[i].getItem() instanceof IElectricItem
                    && this.storage > 0) {
                    sentPacket = ElectricItem.manager
                        .charge(this.chargeSlots[i], this.storage, this.machineTire, false, false);
                    if (sentPacket > 0.0) {
                        needInvUpdate = true;
                    }

                    this.storage = (int) (this.storage - sentPacket);
                }
            }

            if (needInvUpdate) {
                super.markDirty();
            }
        }
    }

    public int gainFuel() {
        if (this.ticker++ % this.tickRate() == 0) {
            this.updateVisibility();
        }

        double raw = this.genDay * this.generationFactor + this.generationRemainder;
        int whole = (int) Math.floor(raw);
        this.generationRemainder = raw - whole;
        this.generating = whole;
        this.active = whole > 0;
        return whole;
    }

    public void updateVisibility() {
        this.generationFactor = SolarMath.generationFactor(worldObj, xCoord, yCoord, zCoord);
        this.sunIsUp = this.generationFactor >= 1.0D;
        this.skyIsVisible = this.generationFactor > 0.0D;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.storage = Math.max(0, Math.min(nbttagcompound.getInteger("storage"), Math.max(1, this.maxStorage)));
        this.generationRemainder = Math.max(0.0D, Math.min(1.0D, nbttagcompound.getDouble("generationRemainder")));
        this.lastX = nbttagcompound.getInteger("lastX");
        this.lastY = nbttagcompound.getInteger("lastY");
        this.lastZ = nbttagcompound.getInteger("lastZ");
        NBTTagList nbttaglist = nbttagcompound.getTagList("Items", 10);
        this.chargeSlots = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if (j >= 0 && j < this.chargeSlots.length) {
                this.chargeSlots[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }

    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        NBTTagList nbttaglist = new NBTTagList();
        nbttagcompound.setInteger("storage", this.storage);
        nbttagcompound.setDouble("generationRemainder", this.generationRemainder);
        nbttagcompound.setInteger("lastX", this.lastX);
        nbttagcompound.setInteger("lastY", this.lastY);
        nbttagcompound.setInteger("lastZ", this.lastZ);

        for (int i = 0; i < this.chargeSlots.length; i++) {
            if (this.chargeSlots[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                this.chargeSlots[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        nbttagcompound.setTag("Items", nbttaglist);
    }

    public boolean isAddedToEnergyNet() {
        return this.addedToEnergyNet;
    }

    public int getMaxEnergyOutput() {
        return this.production;
    }

    public int gaugeEnergyScaled(int i) {
        if (i <= 0 || this.maxStorage <= 0) {
            return 0;
        }
        int scaled = (int) ((long) this.storage * i / this.maxStorage);
        return Math.max(0, Math.min(i, scaled));
    }

    public int gaugeFuelScaled(int i) {
        return i;
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return entityplayer.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64.0;
    }

    public void openInventory() {}

    public void closeInventory() {}

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
        return 0.9F;
    }

    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer) {
        return new ItemStack(
            this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord),
            1,
            this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord));
    }

    public ItemStack[] getContents() {
        return this.chargeSlots;
    }

    public int getSizeInventory() {
        return 4;
    }

    public ItemStack getStackInSlot(int i) {
        return this.chargeSlots[i];
    }

    public ItemStack decrStackSize(int i, int j) {
        if (this.chargeSlots[i] != null) {
            if (this.chargeSlots[i].stackSize <= j) {
                ItemStack itemstack = this.chargeSlots[i];
                this.chargeSlots[i] = null;
                return itemstack;
            } else {
                ItemStack itemstack1 = this.chargeSlots[i].splitStack(j);
                if (this.chargeSlots[i].stackSize == 0) {
                    this.chargeSlots[i] = null;
                }

                return itemstack1;
            }
        } else {
            return null;
        }
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
        this.chargeSlots[i] = itemstack;
        if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit()) {
            itemstack.stackSize = this.getInventoryStackLimit();
        }
        this.markDirty();
    }

    public String getInventoryName() {
        return "Advanced Solar Panel";
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public Container getGuiContainer(InventoryPlayer inventoryplayer) {
        return new ContainerAdvSolarPanel(inventoryplayer, this);
    }

    public String getInvName() {
        return null;
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        if (this.chargeSlots[var1] != null) {
            ItemStack var2 = this.chargeSlots[var1];
            this.chargeSlots[var1] = null;
            return var2;
        } else {
            return null;
        }
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return itemstack != null && itemstack.getItem() instanceof IElectricItem
            && ElectricItem.manager != null
            && ElectricItem.manager.charge(itemstack, Double.POSITIVE_INFINITY, this.machineTire, true, true) > 0.0D;
    }

    public void onNetworkUpdate(String field) {}

    @Override
    public List<String> getNetworkedFields() {
        return fields;
    }

    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true;
    }

    public double getOfferedEnergy() {
        double maxPacket = this.production;
        if (EnergyNet.instance != null) {
            maxPacket = Math.min(maxPacket, EnergyNet.instance.getPowerFromTier(this.machineTire));
        }
        return Math.max(0.0D, Math.min(this.storage, maxPacket));
    }

    public void drawEnergy(double amount) {
        this.storage = Math.max(0, (int) (this.storage - amount));
    }

    public int getSourceTier() {
        return this.machineTire;
    }

    public int getChargeTier() {
        return this.machineTire;
    }
}
