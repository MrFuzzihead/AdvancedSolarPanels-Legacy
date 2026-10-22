package com.mrfuzzihead.advancedsolarpanels.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.advancedsolarpanels.common.container.ContainerQGenerator;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;
import com.mrfuzzihead.advancedsolarpanels.network.PacketGUIPressButton;

public class GuiQGenerator extends GuiContainer {

    private static final ResourceLocation TEX = new ResourceLocation(
        "advancedsolarpanel",
        "textures/gui/GUIQuantumGenerator.png");
    private final TileEntityQGenerator tileentity;

    public GuiQGenerator(InventoryPlayer inventoryplayer, TileEntityQGenerator tileentityqgenerator) {
        super(new ContainerQGenerator(inventoryplayer, tileentityqgenerator));
        this.tileentity = tileentityqgenerator;
        this.xSize = 176;
        this.ySize = 149;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String formatDeviceName = I18n.format("blockQuantumGenerator.name", new Object[0]);
        int nmPos = (this.xSize - this.fontRendererObj.getStringWidth(formatDeviceName)) / 2;
        this.fontRendererObj.drawString(formatDeviceName, nmPos, 6, 16777215);

        String outputString = I18n.format("gui.QuantumGenerator.power", new Object[0]) + ":";
        this.fontRendererObj.drawString(outputString, 54, 24, 16777215);
        String production = Integer.toString(this.tileentity.production);
        this.fontRendererObj
            .drawString(production, 140 - this.fontRendererObj.getStringWidth(production), 25, 16777215);

        String packetString = I18n.format("gui.QuantumGenerator.packet", new Object[0]) + ":";
        this.fontRendererObj.drawString(packetString, 54, 34, 16777215);
        String packet = Integer.toString(this.tileentity.maxPacketSize);
        this.fontRendererObj.drawString(packet, 140 - this.fontRendererObj.getStringWidth(packet), 35, 16777215);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(TEX);
        int h = (this.width - this.xSize) / 2;
        int k = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(h, k, 0, 0, this.xSize, this.ySize);
        if (this.tileentity.active) {
            // active == redstone-powered == generation disabled
            this.drawTexturedModalRect(h + 145, k + 21, 176, 3, 14, 14);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int event = GuiScreen.isShiftKeyDown() ? button.id + 100 : button.id;
        PacketGUIPressButton.issue(this.tileentity, event);
        super.actionPerformed(button);
    }

    @Override
    public void initGui() {
        super.initGui();
        int xGuiPos = (this.width - this.xSize) / 2;
        int yGuiPos = (this.height - this.ySize) / 2;
        this.buttonList.add(new GuiButton(1, xGuiPos + 6, yGuiPos + 40, 32, 20, "-100"));
        this.buttonList.add(new GuiButton(2, xGuiPos + 39, yGuiPos + 40, 26, 20, "-10"));
        this.buttonList.add(new GuiButton(3, xGuiPos + 66, yGuiPos + 40, 20, 20, "-1"));
        this.buttonList.add(new GuiButton(4, xGuiPos + 89, yGuiPos + 40, 20, 20, "+1"));
        this.buttonList.add(new GuiButton(5, xGuiPos + 110, yGuiPos + 40, 26, 20, "+10"));
        this.buttonList.add(new GuiButton(6, xGuiPos + 137, yGuiPos + 40, 32, 20, "+100"));
    }
}
