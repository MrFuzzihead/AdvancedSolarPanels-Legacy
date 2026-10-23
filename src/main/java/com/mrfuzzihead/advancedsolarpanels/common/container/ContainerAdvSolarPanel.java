package com.mrfuzzihead.advancedsolarpanels.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.SlotElectricItem;

public class ContainerAdvSolarPanel extends Container {

    private TileEntitySolarPanel tileentity;
    private int storage = 0;
    private boolean sunIsUp;
    private boolean skyIsVisible;
    private int generating;

    public ContainerAdvSolarPanel(InventoryPlayer inventoryplayer, TileEntitySolarPanel tileentitysolarpanel) {
        this.tileentity = tileentitysolarpanel;

        for (int j = 0; j < 4; j++) {
            this.addSlotToContainer(
                new SlotElectricItem(this.tileentity, j, 17 + j * 18, 59, this.tileentity.getChargeTier()));
        }

        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 9; k++) {
                this.addSlotToContainer(new Slot(inventoryplayer, k + i * 9 + 9, 17 + k * 18, 86 + i * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(inventoryplayer, j, 17 + j * 18, 144));
        }
    }

    public void addCraftingToCrafters(ICrafting icrafting) {
        super.addCraftingToCrafters(icrafting);
        icrafting.sendProgressBarUpdate(this, 0, this.tileentity.sunIsUp ? 1 : 0);
        icrafting.sendProgressBarUpdate(this, 1, this.tileentity.skyIsVisible ? 1 : 0);
        icrafting.sendProgressBarUpdate(this, 2, this.tileentity.generating);
        icrafting.sendProgressBarUpdate(this, 3, this.tileentity.storage & 65535);
        icrafting.sendProgressBarUpdate(this, 4, this.tileentity.storage >>> 16);
        this.sunIsUp = this.tileentity.sunIsUp;
        this.skyIsVisible = this.tileentity.skyIsVisible;
        this.generating = this.tileentity.generating;
        this.storage = this.tileentity.storage;
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        boolean sun = this.tileentity.sunIsUp;
        boolean sky = this.tileentity.skyIsVisible;
        int gen = this.tileentity.generating;
        int stored = this.tileentity.storage;

        for (int i = 0; i < this.crafters.size(); i++) {
            ICrafting icrafting = (ICrafting) this.crafters.get(i);
            if (this.sunIsUp != sun) {
                icrafting.sendProgressBarUpdate(this, 0, sun ? 1 : 0);
            }
            if (this.skyIsVisible != sky) {
                icrafting.sendProgressBarUpdate(this, 1, sky ? 1 : 0);
            }
            if (this.generating != gen) {
                icrafting.sendProgressBarUpdate(this, 2, gen);
            }
            if (this.storage != stored) {
                icrafting.sendProgressBarUpdate(this, 3, stored & 65535);
                icrafting.sendProgressBarUpdate(this, 4, stored >>> 16);
            }
        }

        this.sunIsUp = sun;
        this.skyIsVisible = sky;
        this.generating = gen;
        this.storage = stored;
    }

    public void updateProgressBar(int i, int j) {
        if (i == 0) {
            this.tileentity.sunIsUp = j != 0;
        }

        if (i == 1) {
            this.tileentity.skyIsVisible = j != 0;
        }

        if (i == 2) {
            this.tileentity.generating = j;
        }

        if (i == 3) {
            this.tileentity.storage = this.tileentity.storage & -65536 | j;
        }

        if (i == 4) {
            this.tileentity.storage = this.tileentity.storage & 65535 | j << 16;
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2) {
        ItemStack stack = null;
        Slot slotObject = (Slot) this.inventorySlots.get(par2);
        if (slotObject != null && slotObject.getHasStack()) {
            ItemStack stackInSlot = slotObject.getStack();
            stack = stackInSlot.copy();
            if (par2 < 4) {
                if (!this.mergeItemStack(stackInSlot, 4, 40, true)) {
                    return null;
                }
            } else if (par2 < 31) {
                boolean merged = this.canInsertIntoChargeSlots(stackInSlot)
                    && this.mergeItemStack(stackInSlot, 0, 4, false);
                if (!merged && !this.mergeItemStack(stackInSlot, 31, 40, false)) {
                    return null;
                }
            } else if (par2 < 40) {
                boolean merged = this.canInsertIntoChargeSlots(stackInSlot)
                    && this.mergeItemStack(stackInSlot, 0, 4, false);
                if (!merged && !this.mergeItemStack(stackInSlot, 4, 31, false)) {
                    return null;
                }
            } else {
                return null;
            }

            if (stackInSlot.stackSize == 0) {
                slotObject.putStack(null);
            } else {
                slotObject.onSlotChanged();
            }

            if (stack.stackSize == stackInSlot.stackSize) {
                return null;
            }

            slotObject.onPickupFromSlot(par1EntityPlayer, stackInSlot);
        }

        return stack;
    }

    private boolean canInsertIntoChargeSlots(ItemStack stack) {
        for (int i = 0; i < 4; i++) {
            Slot slot = (Slot) this.inventorySlots.get(i);
            if (slot.isItemValid(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return this.tileentity.isUseableByPlayer(entityplayer);
    }
}
