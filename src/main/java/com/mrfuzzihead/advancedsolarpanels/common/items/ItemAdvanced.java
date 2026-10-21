package com.mrfuzzihead.advancedsolarpanels.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemAdvanced extends Item {

    private final List<String> itemNames = new ArrayList<>();
    private final IIcon[] IIconsList = new IIcon[14];

    public ItemAdvanced() {
        this.setHasSubtypes(true);
        this.setCreativeTab(AdvancedSolarPanel.ic2Tab);
        this.setMaxStackSize(64);
        this.addItemsNames();
    }

    private int clampMetadata(int metadata) {
        return metadata >= 0 && metadata < this.itemNames.size() ? metadata : 0;
    }

    public String getUnlocalizedName(ItemStack stack) {
        return this.itemNames.get(clampMetadata(stack.getItemDamage()));
    }

    public IIcon getIconFromDamage(int par1) {
        return this.IIconsList[clampMetadata(par1)];
    }

    public void addItemsNames() {
        this.itemNames.add("itemSunnarium");
        this.itemNames.add("itemSunnariumAlloy");
        this.itemNames.add("itemIrradiantUranium");
        this.itemNames.add("itemEnrichedSunnarium");
        this.itemNames.add("itemEnrichedSunnariumAlloy");
        this.itemNames.add("itemIrradiantGlassPane");
        this.itemNames.add("itemIridiumIronPlate");
        this.itemNames.add("itemReinforcedIridiumIronPlate");
        this.itemNames.add("itemIrradiantReinforcedPlate");
        this.itemNames.add("itemSunnariumPart");
        this.itemNames.add("ingotIridium");
        this.itemNames.add("itemUranIngot");
        this.itemNames.add("itemMTCore");
        this.itemNames.add("itemQuantumCore");
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister IIconRegister) {
        this.IIconsList[0] = IIconRegister.registerIcon("advancedsolarpanel:Sunnarium");
        this.IIconsList[1] = IIconRegister.registerIcon("advancedsolarpanel:SunnariumAlloy");
        this.IIconsList[2] = IIconRegister.registerIcon("advancedsolarpanel:IrradiantUranium");
        this.IIconsList[3] = IIconRegister.registerIcon("advancedsolarpanel:EnrichedSunnarium");
        this.IIconsList[4] = IIconRegister.registerIcon("advancedsolarpanel:EnrichedSunnariumAlloy");
        this.IIconsList[5] = IIconRegister.registerIcon("advancedsolarpanel:IrradiantGlassPane");
        this.IIconsList[6] = IIconRegister.registerIcon("advancedsolarpanel:IridiumIronPlate");
        this.IIconsList[7] = IIconRegister.registerIcon("advancedsolarpanel:ReinforcedIridiumIronPlate");
        this.IIconsList[8] = IIconRegister.registerIcon("advancedsolarpanel:IrradiantReinforcedPlate");
        this.IIconsList[9] = IIconRegister.registerIcon("advancedsolarpanel:SunnariumPart");
        this.IIconsList[10] = IIconRegister.registerIcon("advancedsolarpanel:IridiumIgnot");
        this.IIconsList[11] = IIconRegister.registerIcon("advancedsolarpanel:UranIngot");
        this.IIconsList[12] = IIconRegister.registerIcon("advancedsolarpanel:MTCore");
        this.IIconsList[13] = IIconRegister.registerIcon("advancedsolarpanel:QuantumCore");
    }

    public void getSubItems(Item item, CreativeTabs tabs, List itemList) {
        for (int meta = 0; meta < this.itemNames.size(); meta++) {
            itemList.add(new ItemStack(this, 1, meta));
        }
    }
}
