package com.mrfuzzihead.advancedsolarpanels.common.items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemMolecularTransformer extends ItemBlock {

    public ItemMolecularTransformer(Block b) {
        super(b);
        this.setMaxDamage(0);
        this.setHasSubtypes(false);
    }

    public String getUnlocalizedName(ItemStack itemstack) {
        return "blockMolecularTransformer";
    }

    public int getMetadata(int i) {
        return i;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        String key = "item.molecularTransformer.desc";
        String translated = StatCollector.translateToLocal(key);
        list.add(translated.equals(key) ? "Converts items using EU." : translated);
    }

    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack itemstack) {
        return EnumRarity.rare;
    }
}
