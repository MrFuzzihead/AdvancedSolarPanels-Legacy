package com.mrfuzzihead.advancedsolarpanels.client.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class NEIASPConfig implements IConfigureNEI {

    public void loadConfig() {
        MinecraftForge.EVENT_BUS.register(this);
        API.registerUsageHandler(new MTRecipeHandler());
        API.registerRecipeHandler(new MTRecipeHandler());
        ItemStack catalyst = AdvancedSolarPanel.itemMolecularTransformer;
        if (catalyst != null) {
            API.addRecipeCatalyst(catalyst, new MTRecipeHandler(), 0);
        }
    }

    @SubscribeEvent
    public void registerHandlerInfo(NEIRegisterHandlerInfosEvent event) {
        HandlerInfo info = new HandlerInfo.Builder(MTRecipeHandler.class, "Advanced Solar Panels", "AdvancedSolarPanel")
            .setDisplayStack(AdvancedSolarPanel.itemMolecularTransformer)
            .setWidth(167)
            .setHeight(67)
            .setMultipleWidgetsAllowed(true)
            .build();
        event.registerHandlerInfo(info);
    }

    public String getName() {
        return "Advanced Solar Panels";
    }

    public String getVersion() {
        return "v1.0";
    }
}
