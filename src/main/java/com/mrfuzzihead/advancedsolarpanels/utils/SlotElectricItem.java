package com.mrfuzzihead.advancedsolarpanels.utils;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

/** Charge slot that only accepts IC2 electric items. */
public class SlotElectricItem extends Slot {

    private final int tier;

    public SlotElectricItem(IInventory inventory, int index, int x, int y) {
        this(inventory, index, x, y, Integer.MAX_VALUE);
    }

    public SlotElectricItem(IInventory inventory, int index, int x, int y, int tier) {
        super(inventory, index, x, y);
        this.tier = tier;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IElectricItem) || ElectricItem.manager == null) {
            return false;
        }
        // Match IC2's InvSlotCharge.accepts(): simulate a charge and reject full/incompatible stacks.
        return ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, this.tier, true, true) > 0.0D;
    }
}
