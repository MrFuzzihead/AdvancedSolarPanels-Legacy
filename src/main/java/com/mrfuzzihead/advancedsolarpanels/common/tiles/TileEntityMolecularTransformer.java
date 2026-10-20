package com.mrfuzzihead.advancedsolarpanels.common.tiles;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.advancedsolarpanels.common.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.network.IReceiveServerEvents;
import com.mrfuzzihead.advancedsolarpanels.network.PacketChangeState;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkTileEntityEventListener;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.tile.IWrenchable;

public class TileEntityMolecularTransformer extends TileEntityBase
    implements IEnergyTile, IWrenchable, IInventory, IEnergySink, ISidedInventory, INetworkDataProvider,
    INetworkUpdateListener, INetworkTileEntityEventListener, IReceiveServerEvents {

    private static final int[] slots_top = new int[] { 0 };
    private static final int[] slots_bottom = new int[] { 1, 1 };
    private static final int[] slots_sides = new int[] { 1 };
    public static Random randomizer = new Random();
    public int ticker;
    public boolean initialized;
    private short facing;
    public boolean addedToEnergyNet;
    private boolean created;
    public boolean doWork;
    public boolean waitOutputSlot;
    public ItemStack lastRecipeInput;
    public ItemStack lastRecipeOutput;
    public int lastRecipeEnergyUsed;
    public int lastRecipeEnergyPerOperation;
    public int lastRecipeNumber;
    private int machineTire;
    private ItemStack[] workSlots;
    private int lastX;
    private int lastY;
    private int lastZ;
    private Boolean deactiveTimer = false;
    private int deactiveTicker = 0;
    private int deactiveTickrate = 40;
    private int energyTicker = 0;
    private int energyTickRate = 60;
    public short lastProgress;
    public int energyBuffer;
    private boolean isActive;
    private boolean prevActiveState;
    public int inputEU;
    public boolean loaded = false;
    private static List<String> fields = Arrays.asList();

    public TileEntityMolecularTransformer() {
        this.created = false;
        this.facing = 2;
        this.workSlots = new ItemStack[2];
        this.initialized = false;
        this.machineTire = Integer.MAX_VALUE;
        this.ticker = randomizer.nextInt(this.tickRate());
        this.lastX = this.xCoord;
        this.lastY = this.yCoord;
        this.lastZ = this.zCoord;
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
        if (!this.worldObj.isRemote && this.addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.addedToEnergyNet = false;
        }

        this.loaded = false;
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

        if (AdvancedSolarPanel.isSimulating()) {
            if (this.lastX != this.xCoord || this.lastZ != this.zCoord || this.lastY != this.yCoord) {
                this.lastX = this.xCoord;
                this.lastY = this.yCoord;
                this.lastZ = this.zCoord;
                this.onUnloaded();
                this.intialize();
            }

            this.flushSlots();
            if (!this.doWork && this.workSlots[0] != null) {
                if (this.canSmelt()) {
                    this.workSlots[0].stackSize = this.workSlots[0].stackSize - this.lastRecipeInput.stackSize;
                    this.lastRecipeEnergyUsed = 0;
                    this.waitOutputSlot = false;
                    this.lastProgress = 0;
                    this.doWork = true;
                    this.deactiveTimer = false;
                }
            } else if (this.doWork) {
                if (this.energyBuffer > 0) {
                    this.energyTicker = 0;
                    if (!this.waitOutputSlot) {
                        this.setActive(true, true);
                    }
                } else {
                    this.energyTicker++;
                    if (this.energyTicker >= this.energyTickRate) {
                        this.energyTicker = 0;
                        if (this.isActive) {
                            this.setActive(false, true);
                        }
                    }
                }

                this.energyBuffer = this.gainFuel(this.energyBuffer);
            }

            if (this.deactiveTimer) {
                this.checkDeactivateMachine();
            }
        }
    }

    public void checkDeactivateMachine() {
        if (this.deactiveTimer) {
            this.deactiveTicker++;
            if (this.deactiveTicker == this.deactiveTickrate) {
                this.deactiveTicker = 0;
                this.deactiveTimer = false;
                this.setActive(false, true);
            }
        }
    }

    private boolean canSmelt() {
        if (this.workSlots[0] == null) {
            return false;
        } else {
            for (int i = 0; i < MTRecipeManager.transformerRecipes.size(); i++) {
                ItemStack tmpItemStack = MTRecipeManager.transformerRecipes.get(i).inputStack;
                if (this.isItemEqual(this.workSlots[0], tmpItemStack)) {
                    if (this.workSlots[1] != null) {
                        if (!this
                            .isItemEqual(this.workSlots[1], MTRecipeManager.transformerRecipes.get(i).outputStack)) {
                            return false;
                        }

                        if (this.workSlots[1].stackSize
                            + MTRecipeManager.transformerRecipes.get(i).outputStack.stackSize
                            > this.workSlots[1].getMaxStackSize()) {
                            return false;
                        }
                    }

                    if (this.workSlots[0].stackSize < MTRecipeManager.transformerRecipes.get(i).inputStack.stackSize) {
                        return false;
                    }

                    this.lastRecipeInput = MTRecipeManager.transformerRecipes.get(i).inputStack.copy();
                    this.lastRecipeOutput = MTRecipeManager.transformerRecipes.get(i).outputStack.copy();
                    this.lastRecipeEnergyPerOperation = MTRecipeManager.transformerRecipes.get(i).energyPerOperation;
                    this.lastRecipeNumber = i;
                    return true;
                }
            }

            return false;
        }
    }

    public int gainFuel(int energyPacket) {
        int energyLeft = energyPacket;
        if (energyPacket >= 0) {
            if (this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed > energyPacket) {
                energyLeft = 0;
                this.lastRecipeEnergyUsed += energyPacket;
            } else {
                energyLeft = energyPacket - (this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed);
                this.lastRecipeEnergyUsed = this.lastRecipeEnergyPerOperation;
                this.lastProgress = 100;
                this.waitOutputSlot = true;
                if (this.workSlots[1] != null) {
                    if (this.isItemEqual(this.lastRecipeOutput, this.workSlots[1])) {
                        if (this.workSlots[1].getMaxStackSize()
                            >= this.workSlots[1].stackSize + this.lastRecipeOutput.stackSize) {
                            this.workSlots[1].stackSize = this.workSlots[1].stackSize + this.lastRecipeOutput.stackSize;
                            this.doWork = false;
                            this.deactiveTicker = 0;
                            this.deactiveTimer = true;
                            super.markDirty();
                            this.waitOutputSlot = false;
                        } else if (this.isActive()) {
                            this.setActive(false, true);
                        }
                    }
                } else {
                    this.workSlots[1] = this.lastRecipeOutput.copy();
                    this.doWork = false;
                    this.deactiveTicker = 0;
                    this.deactiveTimer = true;
                    this.waitOutputSlot = false;
                    super.markDirty();
                }
            }
        }

        this.updateProgress();
        return energyLeft;
    }

    private void updateProgress() {
        if (this.doWork) {
            float tmpProgress = (float) this.lastRecipeEnergyUsed / this.lastRecipeEnergyPerOperation * 100.0F;
            this.lastProgress = (short) Math.round(tmpProgress);
            if (this.lastRecipeEnergyUsed == this.lastRecipeEnergyPerOperation) {
                this.lastProgress = 100;
            }
        } else {
            this.lastProgress = 0;
            if (this.isActive) {
                this.deactiveTicker = 0;
                this.deactiveTimer = true;
            }
        }
    }

    private void flushSlots() {
        if (this.workSlots[0] != null && this.workSlots[0].stackSize <= 0) {
            this.workSlots[0] = null;
        }

        if (this.workSlots[1] != null && this.workSlots[1].stackSize <= 0) {
            this.workSlots[1] = null;
        }
    }

    private boolean isItemEqual(ItemStack inputStack, ItemStack outputStack) {
        return inputStack.getItem() == outputStack.getItem()
            && inputStack.getItemDamage() == outputStack.getItemDamage();
    }

    public void updateVisibility() {}

    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.lastX = nbttagcompound.getInteger("lastX");
        this.lastY = nbttagcompound.getInteger("lastY");
        this.lastZ = nbttagcompound.getInteger("lastZ");
        this.doWork = nbttagcompound.getBoolean("doWork");
        this.lastRecipeEnergyUsed = nbttagcompound.getInteger("lastRecipeEnergyUsed");
        this.lastRecipeEnergyPerOperation = nbttagcompound.getInteger("lastRecipeEnergyPerOperation");
        this.lastProgress = nbttagcompound.getShort("lastProgress");
        NBTTagList nbttaglist_recipe = nbttagcompound.getTagList("Recipes", 10);

        for (int i = 0; i < nbttaglist_recipe.tagCount(); i++) {
            NBTTagCompound nbttagcompound_recipe = nbttaglist_recipe.getCompoundTagAt(i);
            if (i == 0) {
                this.lastRecipeInput = ItemStack.loadItemStackFromNBT(nbttagcompound_recipe);
            }

            if (i == 1) {
                this.lastRecipeOutput = ItemStack.loadItemStackFromNBT(nbttagcompound_recipe);
            }
        }

        if (this.lastRecipeInput != null && this.lastRecipeOutput != null) {
            int i = this.searchRecipeNumber(this.lastRecipeInput, this.lastRecipeOutput);
            if (i < 0) {
                this.lastRecipeNumber = 0;
                this.doWork = false;
                this.lastProgress = 0;
                this.lastRecipeEnergyUsed = 0;
            } else {
                this.lastRecipeNumber = i;
            }
        } else {
            this.lastRecipeNumber = 0;
            this.doWork = false;
            this.lastProgress = 0;
            this.lastRecipeEnergyUsed = 0;
        }

        NBTTagList nbttaglist = nbttagcompound.getTagList("Items", 10);
        this.workSlots = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if (j >= 0 && j < this.workSlots.length) {
                this.workSlots[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }

    public int searchRecipeNumber(ItemStack inputStack, ItemStack outputStack) {
        for (int i = 0; i < MTRecipeManager.transformerRecipes.size(); i++) {
            ItemStack tmpInputStack = MTRecipeManager.transformerRecipes.get(i).inputStack;
            ItemStack tmpOutputStack = MTRecipeManager.transformerRecipes.get(i).outputStack;
            if (this.isItemEqual(inputStack, tmpInputStack) && this.isItemEqual(outputStack, tmpOutputStack)) {
                return i;
            }
        }

        return -1;
    }

    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        NBTTagList nbttaglist = new NBTTagList();
        NBTTagList nbttaglist_recipe = new NBTTagList();
        nbttagcompound.setInteger("lastX", this.lastX);
        nbttagcompound.setInteger("lastY", this.lastY);
        nbttagcompound.setInteger("lastZ", this.lastZ);
        nbttagcompound.setBoolean("doWork", this.doWork);
        nbttagcompound.setInteger("lastRecipeEnergyUsed", this.lastRecipeEnergyUsed);
        nbttagcompound.setInteger("lastRecipeEnergyPerOperation", this.lastRecipeEnergyPerOperation);
        nbttagcompound.setShort("lastProgress", this.lastProgress);
        nbttagcompound.setInteger("lastRecipeNumber", this.lastRecipeNumber);
        NBTTagCompound nbttagcompound_recipe = new NBTTagCompound();
        if (this.lastRecipeInput != null) {
            nbttagcompound_recipe.setBoolean("lastRecipeInput", true);
            this.lastRecipeInput.writeToNBT(nbttagcompound_recipe);
        } else {
            nbttagcompound_recipe.setBoolean("lastRecipeInput", false);
        }

        nbttaglist_recipe.appendTag(nbttagcompound_recipe);
        nbttagcompound_recipe = new NBTTagCompound();
        if (this.lastRecipeOutput != null) {
            nbttagcompound_recipe.setBoolean("lastRecipeOutput", true);
            this.lastRecipeOutput.writeToNBT(nbttagcompound_recipe);
        } else {
            nbttagcompound_recipe.setBoolean("lastRecipeOutput", false);
        }

        nbttaglist_recipe.appendTag(nbttagcompound_recipe);
        nbttagcompound.setTag("Recipes", nbttaglist_recipe);

        for (int i = 0; i < this.workSlots.length; i++) {
            if (this.workSlots[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                this.workSlots[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        nbttagcompound.setTag("Items", nbttaglist);
    }

    public boolean isAddedToEnergyNet() {
        return this.addedToEnergyNet;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean activeState, boolean network) {
        if (network && activeState != this.isActive) {
            PacketChangeState.issue(this, activeState ? 1 : 0, null);
        }

        if (activeState != this.isActive) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }

        this.isActive = activeState;
        if (!activeState) {
            this.inputEU = 0;
        }
    }

    public int gaugeFuelScaled(int i) {
        return i;
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return entityplayer.getDistance(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64.0;
    }

    public int tickRate() {
        return 20;
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

    public ItemStack[] getContents() {
        return this.workSlots;
    }

    public int getSizeInventory() {
        return this.workSlots.length;
    }

    public ItemStack getStackInSlot(int i) {
        return this.workSlots[i];
    }

    public ItemStack decrStackSize(int index, int amount) {
        ItemStack itemStack = this.getStackInSlot(index);
        if (itemStack == null) {
            return null;
        } else if (amount >= itemStack.stackSize) {
            this.setInventorySlotContents(index, null);
            return itemStack;
        } else {
            itemStack.stackSize -= amount;
            ItemStack ret = itemStack.copy();
            ret.stackSize = amount;
            return ret;
        }
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
        this.workSlots[i] = itemstack;
        if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit()) {
            itemstack.stackSize = this.getInventoryStackLimit();
        }
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public Container getGuiContainer(InventoryPlayer inventoryplayer) {
        return new ContainerMolecularTransformer(inventoryplayer, this);
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        if (this.workSlots[var1] != null) {
            ItemStack var2 = this.workSlots[var1];
            this.workSlots[var1] = null;
            return var2;
        } else {
            return null;
        }
    }

    public void onNetworkUpdate(String field) {}

    @Override
    public List<String> getNetworkedFields() {
        return fields;
    }

    @Override
    public void onServerEvent(int event, NBTTagCompound nbtData) {
        Thread.getAllStackTraces();
        switch (event) {
            case 0:
                this.setActive(false, false);
                break;
            case 1:
                this.setActive(true, false);
        }
    }

    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.func_148857_g();
        this.isActive = nbt.getBoolean("active");
    }

    public Packet getDescriptionPacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setBoolean("active", this.isActive);
        this.writeToNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 3, nbttagcompound);
    }

    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
        return true;
    }

    public double getDemandedEnergy() {
        if (!this.doWork) {
            this.inputEU = 0;
            return 0.0;
        } else {
            return this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed >= 0
                ? this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed
                : 0.0;
        }
    }

    public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage) {
        this.inputEU = (int) amount;
        if (!this.doWork) {
            return amount;
        } else if (this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed >= amount) {
            this.energyBuffer = (int) (this.energyBuffer + amount);
            return 0.0;
        } else {
            this.energyBuffer = (int) (this.energyBuffer
                + (amount - (this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed)));
            return amount - (this.lastRecipeEnergyPerOperation - this.lastRecipeEnergyUsed);
        }
    }

    public int getSinkTier() {
        return this.machineTire;
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return i == 1 ? false : i == 0;
    }

    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? slots_bottom : (side == 1 ? slots_top : slots_sides);
    }

    public boolean canInsertItem(int index, ItemStack itemStack, int side) {
        return index != 1;
    }

    public boolean canExtractItem(int index, ItemStack itemStack, int side) {
        return true;
    }

    public String getInventoryName() {
        return "Molecular Transformer";
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    public void openInventory() {}

    public void closeInventory() {}
}
