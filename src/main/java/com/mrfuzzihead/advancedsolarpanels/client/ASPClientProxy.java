package com.mrfuzzihead.advancedsolarpanels.client;

import com.mrfuzzihead.advancedsolarpanels.api.MTAPI;
import com.mrfuzzihead.advancedsolarpanels.client.gui.GuiAdvSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.client.gui.GuiMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.client.gui.GuiQGenerator;
import com.mrfuzzihead.advancedsolarpanels.client.renderers.block.BlockMolecularTransformerRenderer;
import com.mrfuzzihead.advancedsolarpanels.client.renderers.tile.TileMolecularTransformerRenderer;
import com.mrfuzzihead.advancedsolarpanels.common.ASPServerProxy;
import com.mrfuzzihead.advancedsolarpanels.common.AdvancedSolarPanel;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityMolecularTransformer;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntityQGenerator;
import com.mrfuzzihead.advancedsolarpanels.common.tiles.TileEntitySolarPanel;
import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeManager;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ASPClientProxy extends ASPServerProxy {
   public static int[][] sideAndFacingToSpriteOffset;

   @Override
   public void load() {
      try {
         sideAndFacingToSpriteOffset = (int[][])Class.forName("ic2.core.block.BlockMultiID").getField("sideAndFacingToSpriteOffset").get(null);
      } catch (Exception var2) {
         sideAndFacingToSpriteOffset = new int[][]{
            {3, 2, 0, 0, 0, 0}, {2, 3, 1, 1, 1, 1}, {1, 1, 3, 2, 5, 4}, {0, 0, 2, 3, 4, 5}, {4, 5, 4, 5, 3, 2}, {5, 4, 5, 4, 2, 3}
         };
      }
   }

   @Override
   public void registerRenderers() {
      AdvancedSolarPanel.blockMolecularTransformerRenderID = RenderingRegistry.getNextAvailableRenderId();
      RenderingRegistry.registerBlockHandler(new BlockMolecularTransformerRenderer());
      ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMolecularTransformer.class, new TileMolecularTransformerRenderer());
   }

   @Override
   public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
      TileEntity te = world.getTileEntity(X, Y, Z);
      if (te != null) {
         if (te instanceof TileEntitySolarPanel) {
            return new GuiAdvSolarPanel(player.inventory, (TileEntitySolarPanel)te);
         } else if (te instanceof TileEntityQGenerator) {
            return new GuiQGenerator(player.inventory, (TileEntityQGenerator)te);
         } else {
            return te instanceof TileEntityMolecularTransformer ? new GuiMolecularTransformer(player.inventory, (TileEntityMolecularTransformer)te) : null;
         }
      } else {
         return null;
      }
   }

   @Override
   public void initRecipes() {
      MTAPI.manager = MTRecipeManager.instance;
      MTRecipeManager.instance.initRecipes();
   }

   @Override
   public int addArmor(String armorName) {
      return RenderingRegistry.addNewArmourRendererPrefix(armorName);
   }
}
