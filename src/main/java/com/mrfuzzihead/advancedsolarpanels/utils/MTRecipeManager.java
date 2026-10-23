package com.mrfuzzihead.advancedsolarpanels.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.Lists;
import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.api.IMTRecipeManager;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public class MTRecipeManager implements IMTRecipeManager {

    public static List<MTRecipeRecord> transformerRecipes = Lists.newArrayList();
    public static MTRecipeManager instance = new MTRecipeManager();
    private boolean recipesInitialized;
    public static ArrayList<String> defaultRecipeList = new ArrayList<>();
    private static Method getUniqueName_Item;
    private static Method getUniqueName_Block;
    public static boolean rawReflectionDone = false;
    private static final Map<Item, Map<Integer, List<MTRecipeRecord>>> RECIPE_INDEX = new HashMap<>();
    private static final Map<String, List<MTRecipeRecord>> OREDICT_INDEX = new HashMap<>();
    private static int indexedSize = -1;
    private static int indexedSignature = 0;

    public static List<MTRecipeRecord> getRecipesFor(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return Collections.emptyList();
        }

        rebuildIndexIfNeeded();
        Map<Integer, List<MTRecipeRecord>> byDamage = RECIPE_INDEX.get(stack.getItem());
        List<MTRecipeRecord> concrete = byDamage == null ? null : byDamage.get(stack.getItemDamage());
        if (OREDICT_INDEX.isEmpty()) {
            return concrete == null ? Collections.emptyList() : concrete;
        }

        int[] oreIds = OreDictionary.getOreIDs(stack);
        if (oreIds == null || oreIds.length == 0) {
            return concrete == null ? Collections.emptyList() : concrete;
        }

        List<MTRecipeRecord> combined = null;
        for (int oreId : oreIds) {
            String oreName = OreDictionary.getOreName(oreId);
            if (oreName == null) {
                continue;
            }
            List<MTRecipeRecord> oreRecipes = OREDICT_INDEX.get(oreName);
            if (oreRecipes == null || oreRecipes.isEmpty()) {
                continue;
            }
            if (combined == null) {
                combined = new ArrayList<>(concrete == null ? Collections.emptyList() : concrete);
            }
            combined.addAll(oreRecipes);
        }
        if (combined != null) {
            return combined;
        }
        return concrete == null ? Collections.emptyList() : concrete;
    }

    public static boolean matchesInput(ItemStack stack, MTRecipeRecord recipe) {
        if (stack == null || recipe == null) {
            return false;
        }
        if (recipe.inputOreDict != null) {
            for (ItemStack oreStack : OreDictionary.getOres(recipe.inputOreDict)) {
                if (OreDictionary.itemMatches(oreStack, stack, false)) {
                    return true;
                }
            }
            return false;
        }
        ItemStack expected = recipe.inputStack;
        return expected != null && stack.getItem() == expected.getItem()
            && stack.getItemDamage() == expected.getItemDamage()
            && ItemStack.areItemStackTagsEqual(stack, expected);
    }

    private static int indexSignature() {
        int size = transformerRecipes.size();
        if (size == 0) {
            return 0;
        }
        int signature = size;
        signature = 31 * signature + System.identityHashCode(transformerRecipes.get(0));
        signature = 31 * signature + System.identityHashCode(transformerRecipes.get(size - 1));
        if (size > 2) {
            signature = 31 * signature + System.identityHashCode(transformerRecipes.get(size / 2));
        }
        return signature;
    }

    private static void rebuildIndexIfNeeded() {
        int size = transformerRecipes.size();
        int signature = indexSignature();
        if (size == indexedSize && signature == indexedSignature) {
            return;
        }

        RECIPE_INDEX.clear();
        OREDICT_INDEX.clear();
        for (MTRecipeRecord recipe : transformerRecipes) {
            if (recipe == null) {
                continue;
            }
            if (recipe.inputOreDict != null) {
                List<MTRecipeRecord> oreRecipes = OREDICT_INDEX.get(recipe.inputOreDict);
                if (oreRecipes == null) {
                    oreRecipes = new ArrayList<>();
                    OREDICT_INDEX.put(recipe.inputOreDict, oreRecipes);
                }
                oreRecipes.add(recipe);
                continue;
            }
            if (recipe.inputStack == null || recipe.inputStack.getItem() == null) {
                continue;
            }
            Item item = recipe.inputStack.getItem();
            int damage = recipe.inputStack.getItemDamage();
            Map<Integer, List<MTRecipeRecord>> byDamage = RECIPE_INDEX.get(item);
            if (byDamage == null) {
                byDamage = new HashMap<>();
                RECIPE_INDEX.put(item, byDamage);
            }
            List<MTRecipeRecord> recipes = byDamage.get(damage);
            if (recipes == null) {
                recipes = new ArrayList<>();
                byDamage.put(damage, recipes);
            }
            recipes.add(recipe);
        }

        indexedSize = size;
        indexedSignature = signature;
    }

    public static void invalidateRecipeIndex() {
        indexedSize = -1;
        RECIPE_INDEX.clear();
        OREDICT_INDEX.clear();
    }

    public void addMTOreDict(String name, String output, int energy) {
        List<ItemStack> inputs = OreDictionary.getOres(name);
        List<ItemStack> outputs = OreDictionary.getOres(output);
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return;
        }
        for (ItemStack outputIS : outputs) {
            this.addOreDictRecipe(name, inputs.get(0), outputIS, energy);
        }
    }

    public void addMTOreDict(String input, ItemStack output, int energy) {
        List<ItemStack> inputs = OreDictionary.getOres(input);
        if (!inputs.isEmpty()) {
            this.addOreDictRecipe(input, inputs.get(0), output, energy);
        }
    }

    public void addMTOreDict(ItemStack input, String output, int energy) {
        List<ItemStack> outputs = OreDictionary.getOres(output);
        if (outputs.size() != 0) {
            for (ItemStack outputIS : outputs) {
                this.addMTRecipe(input.copy(), outputIS.copy(), energy);
            }
        }
    }

    private void addOreDictRecipe(String inputOreDict, ItemStack representative, ItemStack output, int energy) {
        if (representative == null || output == null) {
            throw new IllegalArgumentException("MT recipes require non-null input and output stacks.");
        }
        if (energy < 0) {
            throw new IllegalArgumentException("MT recipe energy must not be negative: " + energy);
        }
        ItemStack inputStack = representative.copy();
        inputStack.stackSize = clampStackSize(inputStack.stackSize);
        ItemStack outputStack = output.copy();
        outputStack.stackSize = clampStackSize(outputStack.stackSize);

        MTRecipeRecord recipeToAdd = new MTRecipeRecord();
        recipeToAdd.inputOreDict = inputOreDict;
        recipeToAdd.inputStack = inputStack;
        recipeToAdd.outputStack = outputStack;
        recipeToAdd.energyPerOperation = energy;
        transformerRecipes.add(recipeToAdd);
    }

    public void initRecipes() {
        if (recipesInitialized) {
            return;
        }
        if (AdvancedSolarPanel.configFileName == null || AdvancedSolarPanel.configFileName.isEmpty()) {
            AdvancedSolarPanel.addLog("ASP config path is unavailable; MT recipes were not loaded.");
            return;
        }
        recipesInitialized = true;

        String configFilePath = AdvancedSolarPanel.configFileName
            .substring(0, AdvancedSolarPanel.configFileName.lastIndexOf(File.separatorChar) + 1);
        String tmpFileName = AdvancedSolarPanel.configFileName
            .substring(AdvancedSolarPanel.configFileName.lastIndexOf(File.separatorChar) + 1);
        int dotIndex = tmpFileName.lastIndexOf('.');
        String recipesFileName = dotIndex >= 0 ? tmpFileName.substring(0, dotIndex) : tmpFileName;
        String recipesFileExt = dotIndex >= 0 ? tmpFileName.substring(dotIndex) : ".cfg";
        recipesFileName = recipesFileName + "_MTRecipes" + recipesFileExt;
        File file = new File(configFilePath, recipesFileName);
        transformerRecipes.addAll(MTRecipeConfig.parse(file));
    }

    private static int clampStackSize(int stackSize) {
        return Math.max(1, Math.min(64, stackSize));
    }

    private static void lazyReflectionInit() {
        if (!rawReflectionDone) {
            try {
                getUniqueName_Item = GameData.class.getDeclaredMethod("getUniqueName", Item.class);
                getUniqueName_Block = GameData.class.getDeclaredMethod("getUniqueName", Block.class);
                getUniqueName_Item.setAccessible(true);
                getUniqueName_Block.setAccessible(true);
            } catch (Exception var1) {
                AdvancedSolarPanel.addLog("Reflection failed. This is a fatal error and not recoverable");
                throw new RuntimeException(var1);
            }

            rawReflectionDone = true;
        }
    }

    public static MTRecipeManager.RawItemData getItemData(ItemStack is) {
        lazyReflectionInit();

        try {
            Item i = is.getItem();
            if (i instanceof ItemBlock) {
                Block b = Block.getBlockFromItem(i);
                UniqueIdentifier ui = (UniqueIdentifier) getUniqueName_Block.invoke(null, b);
                return new MTRecipeManager.RawItemData(ui.modId, ui.name);
            } else {
                UniqueIdentifier ui = (UniqueIdentifier) getUniqueName_Item.invoke(null, i);
                return new MTRecipeManager.RawItemData(ui.modId, ui.name);
            }
        } catch (Throwable var4) {
            AdvancedSolarPanel.addLog("Reflection failed. Weird error, report it.");
            var4.printStackTrace();
            return null;
        }
    }

    @Override
    public void removeMTRecipe(ItemStack inputItem, ItemStack outputItem) {
        if (outputItem == null) {
            return;
        }
        Iterator<MTRecipeRecord> iterator = transformerRecipes.iterator();
        while (iterator.hasNext()) {
            MTRecipeRecord recipe = iterator.next();
            if (recipe != null && matchesInput(inputItem, recipe) && stackMatches(recipe.outputStack, outputItem)) {
                iterator.remove();
            }
        }
        invalidateRecipeIndex();
    }

    @Override
    public void clearMTRecipes() {
        transformerRecipes.clear();
        invalidateRecipeIndex();
    }

    @Override
    public List<MTRecipeRecord> getMTRecipes() {
        return Collections.unmodifiableList(transformerRecipes);
    }

    private static boolean stackMatches(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.getItem() == second.getItem()
            && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    @Override
    public void addMTRecipe(ItemStack inputItem, ItemStack outputItem, int energyPerOperation) {
        if (inputItem == null || outputItem == null) {
            throw new IllegalArgumentException("MT recipes require non-null input and output stacks.");
        }
        if (energyPerOperation < 0) {
            throw new IllegalArgumentException("MT recipe energy must not be negative: " + energyPerOperation);
        }

        ItemStack inputStack = inputItem.copy();
        inputStack.stackSize = clampStackSize(inputStack.stackSize);
        ItemStack outputStack = outputItem.copy();
        outputStack.stackSize = clampStackSize(outputStack.stackSize);

        MTRecipeRecord recipeToAdd = new MTRecipeRecord();
        recipeToAdd.inputStack = inputStack;
        recipeToAdd.outputStack = outputStack;
        recipeToAdd.energyPerOperation = energyPerOperation;
        transformerRecipes.add(recipeToAdd);
        invalidateRecipeIndex();
    }

    public static class RawItemData {

        public final String modId;
        public final String itemName;

        public RawItemData(String id, String name) {
            this.modId = id;
            this.itemName = name;
        }
    }
}
