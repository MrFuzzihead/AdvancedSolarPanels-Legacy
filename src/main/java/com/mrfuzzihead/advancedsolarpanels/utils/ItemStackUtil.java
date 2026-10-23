package com.mrfuzzihead.advancedsolarpanels.utils;

import net.minecraft.item.ItemStack;

public class ItemStackUtil {

    public static boolean areStacksEqual(ItemStack first, ItemStack second) {
        return first != null && second != null ? first.getItem() == second.getItem()
            && (first.getItemDamage() == second.getItemDamage() || first.getItemDamage() == 32767
                || second.getItemDamage() == 32767)
            : false;
    }
}
