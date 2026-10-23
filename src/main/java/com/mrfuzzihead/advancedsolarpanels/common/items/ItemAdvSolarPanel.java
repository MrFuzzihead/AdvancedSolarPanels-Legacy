package com.mrfuzzihead.advancedsolarpanels.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemAdvSolarPanel extends ItemBlock {

    public static final int MAX_METADATA = 4;
    private final List<String> itemNames;

    public ItemAdvSolarPanel(Block b) {
        super(b);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
        this.itemNames = new ArrayList<>();
        this.addItemsNames();
    }

    public int getMetadata(int i) {
        return i >= 0 && i <= MAX_METADATA ? i : 0;
    }

    public String getUnlocalizedName(ItemStack itemstack) {
        int metadata = itemstack.getItemDamage();
        if (metadata < 0 || metadata >= this.itemNames.size()) {
            metadata = 0;
        }
        return this.itemNames.get(metadata);
    }

    public void addItemsNames() {
        this.itemNames.add("blockAdvancedSolarPanel");
        this.itemNames.add("blockHybridSolarPanel");
        this.itemNames.add("blockUltimateSolarPanel");
        this.itemNames.add("blockQuantumSolarPanel");
        this.itemNames.add("blockQuantumGenerator");
    }

    public static int panelDayOutput(int metadata) {
        switch (metadata) {
            case 0:
                return AdvancedSolarPanel.advGenDay;
            case 1:
                return AdvancedSolarPanel.hGenDay;
            case 2:
                return AdvancedSolarPanel.uhGenDay;
            case 3:
                return AdvancedSolarPanel.qpGenDay;
            default:
                return 0;
        }
    }

    public static int panelTier(int metadata) {
        switch (metadata) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 5;
            default:
                return 0;
        }
    }

    public static int panelStorage(int metadata) {
        switch (metadata) {
            case 0:
                return AdvancedSolarPanel.advStorage;
            case 1:
                return AdvancedSolarPanel.hStorage;
            case 2:
                return AdvancedSolarPanel.uhStorage;
            case 3:
                return AdvancedSolarPanel.qpStorage;
            default:
                return 0;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        int metadata = stack.getItemDamage();
        if (metadata == 4) {
            list.add(
                StatCollector.translateToLocal("ic2.item.tooltip.PowerOutput") + " "
                    + AdvancedSolarPanel.qgbaseProduction
                    + " EU/t "
                    + StatCollector.translateToLocal("ic2.item.tooltip.max"));
            list.add(
                StatCollector.translateToLocal("gui.QuantumGenerator.packet") + ": "
                    + AdvancedSolarPanel.qgbaseMaxPacketSize);
            return;
        }
        if (metadata < 0 || metadata > 3) {
            return;
        }
        list.add(
            StatCollector.translateToLocal("ic2.item.tooltip.PowerOutput") + " "
                + panelDayOutput(metadata)
                + " EU/t "
                + StatCollector.translateToLocal("ic2.item.tooltip.max"));
        list.add(StatCollector.translateToLocal("ic2.item.tooltip.PowerTier") + " " + panelTier(metadata));
        list.add(
            StatCollector.translateToLocal("gui.AdvancedSolarPanel.storage") + ": " + panelStorage(metadata) + " EU");
    }

    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack itemstack) {
        int i = itemstack.getItemDamage();
        switch (i) {
            case 0:
                return EnumRarity.uncommon;
            case 1:
                return EnumRarity.rare;
            case 2:
                return EnumRarity.epic;
            case 3:
                return EnumRarity.epic;
            case 4:
                return EnumRarity.epic;
            default:
                return EnumRarity.uncommon;
        }
    }
}
