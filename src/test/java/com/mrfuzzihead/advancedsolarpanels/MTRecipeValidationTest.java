package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeRecord;

class MTRecipeValidationTest {

    @BeforeEach
    void clearRecipes() {
        MTRecipeManager.transformerRecipes.clear();
    }

    private static ItemStack stack(int size) {
        return new ItemStack(new Item(), size, 0);
    }

    @Test
    void rejectsNullStacks() {
        assertThrows(IllegalArgumentException.class, () -> MTRecipeManager.instance.addMTRecipe(null, stack(1), 1));
        assertThrows(IllegalArgumentException.class, () -> MTRecipeManager.instance.addMTRecipe(stack(1), null, 1));
    }

    @Test
    void rejectsNegativeEnergy() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MTRecipeManager.instance.addMTRecipe(stack(1), stack(1), -1));
    }

    @Test
    void clampsZeroAndOversizedStacks() {
        MTRecipeManager.instance.addMTRecipe(stack(0), stack(999), 10);
        assertEquals(1, MTRecipeManager.transformerRecipes.size());
        MTRecipeRecord recipe = MTRecipeManager.transformerRecipes.get(0);
        assertNotNull(recipe.inputStack);
        assertNotNull(recipe.outputStack);
        assertEquals(1, recipe.inputStack.stackSize);
        assertEquals(64, recipe.outputStack.stackSize);
    }

    @Test
    void indexesRecipesByItemAndDamage() {
        Item item = new Item();
        MTRecipeManager.instance.addMTRecipe(new ItemStack(item, 1, 0), stack(1), 10);
        MTRecipeManager.instance.addMTRecipe(new ItemStack(item, 1, 1), stack(1), 20);

        assertEquals(
            1,
            MTRecipeManager.getRecipesFor(new ItemStack(item, 1, 0))
                .size());
        assertEquals(
            1,
            MTRecipeManager.getRecipesFor(new ItemStack(item, 1, 1))
                .size());
        assertEquals(
            0,
            MTRecipeManager.getRecipesFor(new ItemStack(item, 1, 2))
                .size());
    }

    @Test
    void copiesStacksInsteadOfAliasingThem() {
        ItemStack input = stack(2);
        ItemStack output = stack(1);
        MTRecipeManager.instance.addMTRecipe(input, output, 10);

        input.stackSize = 5;
        output.stackSize = 5;

        MTRecipeRecord recipe = MTRecipeManager.transformerRecipes.get(0);
        assertEquals(2, recipe.inputStack.stackSize);
        assertEquals(1, recipe.outputStack.stackSize);
    }
}
