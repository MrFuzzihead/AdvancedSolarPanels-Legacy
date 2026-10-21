package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.SlotElectricItem;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;

class SlotElectricItemTest {

    private IElectricItemManager previousManager;

    @BeforeEach
    void installManager() {
        previousManager = ElectricItem.manager;
        ElectricItem.manager = new TestElectricItemManager();
    }

    @AfterEach
    void restoreManager() {
        ElectricItem.manager = previousManager;
    }

    @Test
    void acceptsOnlyElectricItems() {
        SlotElectricItem slot = new SlotElectricItem(null, 0, 0, 0);

        assertTrue(slot.isItemValid(new ItemStack(new TestElectricItem(), 1, 0)));
        assertFalse(slot.isItemValid(new ItemStack(new Item(), 1, 0)));
        assertFalse(slot.isItemValid(null));
    }

    @Test
    void rejectsFullElectricItems() {
        SlotElectricItem slot = new SlotElectricItem(null, 0, 0, 0);
        ItemStack stack = new ItemStack(new TestElectricItem(), 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("charge", 1_000.0D);
        stack.setTagCompound(tag);

        assertFalse(slot.isItemValid(stack));
    }

    @Test
    void rejectsStackedElectricItems() {
        SlotElectricItem slot = new SlotElectricItem(null, 0, 0, 0);
        assertFalse(slot.isItemValid(new ItemStack(new TestElectricItem(), 2, 0)));
    }

    private static final class TestElectricItemManager implements IElectricItemManager {

        @Override
        public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
            IElectricItem item = (IElectricItem) stack.getItem();
            double current = stack.hasTagCompound() ? stack.getTagCompound()
                .getDouble("charge") : 0.0D;
            if (stack.stackSize > 1 || item.getTier(stack) > tier) {
                return 0.0D;
            }
            return Math.min(amount, item.getMaxCharge(stack) - current);
        }

        @Override
        public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit,
            boolean externally, boolean simulate) {
            return 0.0D;
        }

        @Override
        public double getCharge(ItemStack stack) {
            return stack.hasTagCompound() ? stack.getTagCompound()
                .getDouble("charge") : 0.0D;
        }

        @Override
        public boolean canUse(ItemStack stack, double amount) {
            return false;
        }

        @Override
        public boolean use(ItemStack stack, double amount, EntityLivingBase entity) {
            return false;
        }

        @Override
        public void chargeFromArmor(ItemStack stack, EntityLivingBase entity) {}

        @Override
        public String getToolTip(ItemStack stack) {
            return "";
        }
    }

    private static final class TestElectricItem extends Item implements IElectricItem {

        @Override
        public boolean canProvideEnergy(ItemStack stack) {
            return false;
        }

        @Override
        public Item getChargedItem(ItemStack stack) {
            return this;
        }

        @Override
        public Item getEmptyItem(ItemStack stack) {
            return this;
        }

        @Override
        public double getMaxCharge(ItemStack stack) {
            return 1_000.0D;
        }

        @Override
        public int getTier(ItemStack stack) {
            return 1;
        }

        @Override
        public double getTransferLimit(ItemStack stack) {
            return 32.0D;
        }
    }
}
