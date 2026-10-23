package com.mrfuzzihead.advancedsolarpanels.api;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeRecord;

public interface IMTRecipeManager {

    void addMTRecipe(ItemStack input, ItemStack output, int energyPerOperation);

    default void removeMTRecipe(ItemStack input, ItemStack output) {}

    default void clearMTRecipes() {}

    default List<MTRecipeRecord> getMTRecipes() {
        return Collections.emptyList();
    }
}
