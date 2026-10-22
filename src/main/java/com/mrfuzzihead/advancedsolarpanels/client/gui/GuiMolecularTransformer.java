package com.mrfuzzihead.advancedsolarpanels.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeRecord;

import cpw.mods.fml.client.FMLClientHandler;

public class GuiMolecularTransformer extends GuiContainer {

    public TileEntityMolecularTransformer tileentity;
    private int maxTextXPos;
    private static ResourceLocation tex = new ResourceLocation(
        "advancedsolarpanel",
        "textures/gui/guiMolecularTransformerNew.png");
    private final List<GuiMolecularTransformer.captionRecord> guiTextList = Lists.newArrayList();
    public Minecraft mc = FMLClientHandler.instance()
        .getClient();

    public GuiMolecularTransformer(InventoryPlayer inventoryplayer,
        TileEntityMolecularTransformer tileentitymoleculartransformer) {
        super(new ContainerMolecularTransformer(inventoryplayer, tileentitymoleculartransformer));
        this.tileentity = tileentitymoleculartransformer;
        this.allowUserInput = false;
        this.xSize = 220;
        this.ySize = 193;
        this.setCaptionText();
    }

    public void setCaptionText() {
        this.guiTextList.clear();
        GuiMolecularTransformer.captionRecord textToAdd = new GuiMolecularTransformer.captionRecord();
        textToAdd.textCaption = I18n.format("gui.MolecularTransformer.input", new Object[0]) + ": ";
        textToAdd.textWidth = this.mc.fontRenderer.getStringWidth(textToAdd.textCaption);
        guiTextList.add(textToAdd);
        textToAdd = new GuiMolecularTransformer.captionRecord();
        textToAdd.textCaption = I18n.format("gui.MolecularTransformer.output", new Object[0]) + ": ";
        textToAdd.textWidth = this.mc.fontRenderer.getStringWidth(textToAdd.textCaption);
        guiTextList.add(textToAdd);
        textToAdd = new GuiMolecularTransformer.captionRecord();
        textToAdd.textCaption = I18n.format("gui.MolecularTransformer.energyPerOperation", new Object[0]) + ": ";
        textToAdd.textWidth = this.mc.fontRenderer.getStringWidth(textToAdd.textCaption);
        guiTextList.add(textToAdd);
        textToAdd = new GuiMolecularTransformer.captionRecord();
        textToAdd.textCaption = I18n.format("gui.AdvancedSolarPanel.energyPerTick", new Object[0]) + ": ";
        textToAdd.textWidth = this.mc.fontRenderer.getStringWidth(textToAdd.textCaption);
        guiTextList.add(textToAdd);
        textToAdd = new GuiMolecularTransformer.captionRecord();
        textToAdd.textCaption = I18n.format("gui.MolecularTransformer.progress", new Object[0]) + ": ";
        textToAdd.textWidth = this.mc.fontRenderer.getStringWidth(textToAdd.textCaption);
        guiTextList.add(textToAdd);
        this.maxTextXPos = 0;

        for (int i = 0; i < guiTextList.size(); i++) {
            if (guiTextList.get(i).textWidth > this.maxTextXPos) {
                this.maxTextXPos = guiTextList.get(i).textWidth;
            }
        }
    }

    private MTRecipeRecord getDisplayedRecipe() {
        int index = this.tileentity.lastRecipeNumber;
        if (index < 0 || index >= MTRecipeManager.transformerRecipes.size()) {
            return null;
        }
        return MTRecipeManager.transformerRecipes.get(index);
    }

    public static String parsingNumber(String number) {
        String tmpString = "";
        int count = 0;

        for (int i = number.length() - 1; i >= 0; i--) {
            if (count == 3) {
                tmpString = " " + tmpString;
                count = 0;
            }

            count++;
            tmpString = number.charAt(i) + tmpString;
        }

        return tmpString;
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        int xOffset = 56;
        int yOffset = 26;
        int yTextInterval = 12;
        String formatDeviceName = I18n.format("blockMolecularTransformer.name", new Object[0]);
        int nmPos = (this.xSize - this.fontRendererObj.getStringWidth(formatDeviceName)) / 2;
        this.fontRendererObj.drawString(formatDeviceName, nmPos, 8, 16777215);
        if (this.tileentity.lastProgress <= 0 && !this.tileentity.doWork) {
            this.fontRendererObj.drawString(
                guiTextList.get(0).textCaption,
                xOffset + this.maxTextXPos - guiTextList.get(0).textWidth,
                yOffset,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(1).textCaption,
                xOffset + this.maxTextXPos - guiTextList.get(1).textWidth,
                yOffset + yTextInterval * 1,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(2).textCaption,
                xOffset + this.maxTextXPos - guiTextList.get(2).textWidth,
                yOffset + yTextInterval * 2,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(3).textCaption,
                xOffset + this.maxTextXPos - guiTextList.get(3).textWidth,
                yOffset + yTextInterval * 3,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(4).textCaption,
                xOffset + this.maxTextXPos - guiTextList.get(4).textWidth,
                yOffset + yTextInterval * 4,
                16777215);
        } else {
            ItemStack inputStack = this.tileentity.lastRecipeInput;
            ItemStack outputStack = this.tileentity.lastRecipeOutput;
            int energyPerOperation = this.tileentity.lastRecipeEnergyPerOperation;
            if (inputStack == null || outputStack == null) {
                MTRecipeRecord recipe = this.getDisplayedRecipe();
                if (recipe != null) {
                    inputStack = recipe.inputStack;
                    outputStack = recipe.outputStack;
                    energyPerOperation = recipe.energyPerOperation;
                }
            }
            String inputName = inputStack != null ? inputStack.getDisplayName() : "?";
            String outputName = outputStack != null ? outputStack.getDisplayName() : "?";
            String inputEuFormated = parsingNumber(String.valueOf(this.tileentity.inputEU));
            String energyPerOpFormated = parsingNumber(String.valueOf(energyPerOperation));
            int progress = Math.max(0, Math.min(100, this.tileentity.lastProgress));
            this.fontRendererObj.drawString(
                guiTextList.get(0).textCaption + inputName,
                xOffset + this.maxTextXPos - guiTextList.get(0).textWidth,
                yOffset,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(1).textCaption + outputName,
                xOffset + this.maxTextXPos - guiTextList.get(1).textWidth,
                yOffset + yTextInterval * 1,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(2).textCaption + energyPerOpFormated + " EU",
                xOffset + this.maxTextXPos - guiTextList.get(2).textWidth,
                yOffset + yTextInterval * 2,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(3).textCaption + inputEuFormated,
                xOffset + this.maxTextXPos - guiTextList.get(3).textWidth,
                yOffset + yTextInterval * 3,
                16777215);
            this.fontRendererObj.drawString(
                guiTextList.get(4).textCaption + progress + "%",
                xOffset + this.maxTextXPos - guiTextList.get(4).textWidth,
                yOffset + yTextInterval * 4,
                16777215);
        }
    }

    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(tex);
        int h = (this.width - this.xSize) / 2;
        int k = (this.height - this.ySize) / 2;
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        this.drawTexturedModalRect(h, k, 0, 0, this.xSize, this.ySize);
        GL11.glDisable(3042);
        int progress = Math.max(0, Math.min(100, this.tileentity.lastProgress));
        if (progress > 0) {
            int l = progress * 15 / 100;
            this.drawTexturedModalRect(h + 23, k + 48, 221, 7, 10, l + 1);
        }
    }

    public static class captionRecord {

        public String textCaption;
        public int textWidth;
    }
}
