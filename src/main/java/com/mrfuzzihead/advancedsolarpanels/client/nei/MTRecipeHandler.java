package com.mrfuzzihead.advancedsolarpanels.client.nei;

import java.awt.Rectangle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.advancedsolarpanels.client.gui.GuiMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.utils.ItemStackUtil;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeRecord;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class MTRecipeHandler extends TemplateRecipeHandler {

    public int ticks;

    public void onUpdate() {
        super.onUpdate();
        this.ticks++;
    }

    public void drawBackground(int i) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiDraw.changeTexture(this.getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 167, 67);
    }

    public void loadTransferRects() {
        this.transferRects
            .add(new RecipeTransferRect(new Rectangle(17, 35, 12, 12), this.getRecipeId(), new Object[0]));
    }

    public String getRecipeName() {
        String key = "nei.advancedsolarpanel.molecular_transformer";
        String translated = StatCollector.translateToLocal(key);
        return translated.equals(key) ? "Molecular Transformer" : translated;
    }

    public String getRecipeId() {
        return "advancedsolarpanel.molecular_transformer";
    }

    @Override
    public String getOverlayIdentifier() {
        return this.getRecipeId();
    }

    public void drawExtras(int r) {
        float f = this.ticks >= 20 ? (this.ticks - 20) % 20 / 20.0F : 0.0F;
        this.drawProgressBar(15, 29, 177, 3, 10, 9, f, 1);
        MTRecipeHandler.CachedMTRecipe recipe = (MTRecipeHandler.CachedMTRecipe) this.arecipes.get(r);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        ItemStack inputStack = recipe.input;
        ItemStack outputStack = recipe.output;
        int energy = recipe.energy;
        String formatedEnergy = GuiMolecularTransformer.parsingNumber(String.valueOf(energy));
        String inputLabel = StatCollector.translateToLocal("gui.MolecularTransformer.input") + ": ";
        String outputLabel = StatCollector.translateToLocal("gui.MolecularTransformer.output") + ": ";
        String energyLabel = StatCollector.translateToLocal("gui.MolecularTransformer.energyPerOperation") + ": ";
        fontRenderer.drawString(inputLabel + inputStack.getDisplayName(), 46, 18, 16777215);
        fontRenderer.drawString(outputLabel + outputStack.getDisplayName(), 46, 28, 16777215);
        fontRenderer.drawString(energyLabel + formatedEnergy, 46, 38, 16777215);
    }

    public String getGuiTexture() {
        return "advancedsolarpanel:textures/gui/guiMolecularTransformer_nei.png";
    }

    public void loadUsageRecipes(ItemStack ingredient) {
        for (MTRecipeRecord record : MTRecipeManager.transformerRecipes) {
            if (MTRecipeManager.matchesInput(ingredient, record)) {
                this.arecipes.add(
                    new MTRecipeHandler.CachedMTRecipe(
                        record.inputStack,
                        record.outputStack,
                        record.energyPerOperation));
            }
        }
    }

    public void loadCraftingRecipes(String outputId, Object... results) {
        if (this.getRecipeId()
            .equals(outputId)) {
            for (MTRecipeRecord record : MTRecipeManager.transformerRecipes) {
                this.arecipes.add(
                    new MTRecipeHandler.CachedMTRecipe(
                        record.inputStack,
                        record.outputStack,
                        record.energyPerOperation));
            }
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    public void loadCraftingRecipes(ItemStack ingredient) {
        for (MTRecipeRecord record : MTRecipeManager.transformerRecipes) {
            if (ItemStackUtil.areStacksEqual(record.outputStack, ingredient)) {
                this.arecipes.add(
                    new MTRecipeHandler.CachedMTRecipe(
                        record.inputStack,
                        record.outputStack,
                        record.energyPerOperation));
            }
        }
    }

    public int recipiesPerPage() {
        return 2;
    }

    public Class<? extends GuiContainer> getGuiClass() {
        return GuiMolecularTransformer.class;
    }

    public class CachedMTRecipe extends CachedRecipe {

        public ItemStack input;
        public ItemStack output;
        public int energy;

        public CachedMTRecipe(ItemStack input, ItemStack output, int energy) {
            this.input = input;
            this.output = output;
            this.energy = energy;
        }

        public PositionedStack getIngredient() {
            return new PositionedStack(this.input, 12, 8);
        }

        public PositionedStack getResult() {
            return new PositionedStack(this.output, 12, 43);
        }
    }
}
