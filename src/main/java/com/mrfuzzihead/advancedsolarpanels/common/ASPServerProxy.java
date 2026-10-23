package com.mrfuzzihead.advancedsolarpanels.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.mrfuzzihead.advancedsolarpanels.api.MTAPI;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;

import cpw.mods.fml.common.network.IGuiHandler;

public class ASPServerProxy implements IGuiHandler {

    public void load() {}

    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        TileEntity te = world.getTileEntity(X, Y, Z);
        if (te != null) {
            if (te instanceof TileEntitySolarPanel) {
                return ((TileEntitySolarPanel) te).getGuiContainer(player.inventory);
            } else if (te instanceof TileEntityQGenerator) {
                return ((TileEntityQGenerator) te).getGuiContainer(player.inventory);
            } else {
                return te instanceof TileEntityMolecularTransformer
                    ? ((TileEntityMolecularTransformer) te).getGuiContainer(player.inventory)
                    : null;
            }
        } else {
            return null;
        }
    }

    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        return null;
    }

    public void initRecipes() {
        MTAPI.manager = MTRecipeManager.instance;
        MTRecipeManager.instance.initRecipes();
    }

    public int addArmor(String armorName) {
        return 0;
    }

    public void registerRenderers() {}
}
