package com.mrfuzzihead.advancedsolarpanels.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;

public class ContainerQGenerator extends Container {

    private TileEntityQGenerator tileentity;
    private int lastProduction = -1;
    private int lastMaxPacketSize = -1;
    private int lastActive = -1;

    public ContainerQGenerator(InventoryPlayer inventoryplayer, TileEntityQGenerator tileentityqgenerator) {
        this.tileentity = tileentityqgenerator;

        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 9; k++) {
                this.addSlotToContainer(new Slot(inventoryplayer, k + i * 9 + 9, 8 + k * 18, 65 + i * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(inventoryplayer, j, 8 + j * 18, 123));
        }
    }

    public void addCraftingToCrafters(ICrafting icrafting) {
        super.addCraftingToCrafters(icrafting);
        icrafting.sendProgressBarUpdate(this, 0, this.tileentity.production);
        icrafting.sendProgressBarUpdate(this, 1, this.tileentity.maxPacketSize);
        icrafting.sendProgressBarUpdate(this, 2, this.tileentity.active ? 1 : 0);
        this.lastProduction = this.tileentity.production;
        this.lastMaxPacketSize = this.tileentity.maxPacketSize;
        this.lastActive = this.tileentity.active ? 1 : 0;
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.crafters.size(); i++) {
            ICrafting icrafting = (ICrafting) this.crafters.get(i);
            if (this.lastProduction != this.tileentity.production) {
                icrafting.sendProgressBarUpdate(this, 0, this.tileentity.production);
            }
            if (this.lastMaxPacketSize != this.tileentity.maxPacketSize) {
                icrafting.sendProgressBarUpdate(this, 1, this.tileentity.maxPacketSize);
            }
            int active = this.tileentity.active ? 1 : 0;
            if (this.lastActive != active) {
                icrafting.sendProgressBarUpdate(this, 2, active);
            }
        }

        this.lastProduction = this.tileentity.production;
        this.lastMaxPacketSize = this.tileentity.maxPacketSize;
        this.lastActive = this.tileentity.active ? 1 : 0;
    }

    public void updateProgressBar(int i, int j) {
        if (i == 0) {
            this.tileentity.production = j;
        }

        if (i == 1) {
            this.tileentity.maxPacketSize = j;
        }

        if (i == 2) {
            this.tileentity.active = j != 0;
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = null;
        Slot slot = (Slot) this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();

            if (index < 27) {
                if (!this.mergeItemStack(stack, 27, 36, false)) {
                    return null;
                }
            } else if (!this.mergeItemStack(stack, 0, 27, false)) {
                return null;
            }

            if (stack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (stack.stackSize == result.stackSize) {
                return null;
            }
            slot.onPickupFromSlot(player, stack);
        }

        return result;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return this.tileentity.isUseableByPlayer(entityplayer);
    }
}
