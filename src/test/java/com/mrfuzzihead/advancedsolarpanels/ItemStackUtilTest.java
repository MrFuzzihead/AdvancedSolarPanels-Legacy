package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.ItemStackUtil;

class ItemStackUtilTest {

    @Test
    void comparesItemAndDamageWithWildcardSupport() {
        Item item = new Item();
        assertTrue(ItemStackUtil.areStacksEqual(new ItemStack(item, 1, 4), new ItemStack(item, 1, 4)));
        assertTrue(ItemStackUtil.areStacksEqual(new ItemStack(item, 1, 32767), new ItemStack(item, 1, 9)));
        assertTrue(ItemStackUtil.areStacksEqual(new ItemStack(item, 1, 9), new ItemStack(item, 1, 32767)));
        assertFalse(ItemStackUtil.areStacksEqual(new ItemStack(item, 1, 4), new ItemStack(item, 1, 5)));
    }

    @Test
    void ignoresNbtAndRejectsNulls() {
        Item item = new Item();
        ItemStack tagged = new ItemStack(item, 1, 0);
        tagged.stackTagCompound = new NBTTagCompound();
        tagged.stackTagCompound.setInteger("marker", 1);

        assertTrue(ItemStackUtil.areStacksEqual(tagged, new ItemStack(item, 1, 0)));
        assertFalse(ItemStackUtil.areStacksEqual(null, new ItemStack(item, 1, 0)));
        assertFalse(ItemStackUtil.areStacksEqual(new ItemStack(item, 1, 0), null));
    }
}
