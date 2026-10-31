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

        // NEI posts the initial NEIRegisterHandlerInfosEvent before loading the plugins that subscribe to it,
        // so the event handler below is too late for the first handler-info pass. Register directly here;
        // the event subscription still covers later reloads (for example the "load handlers from config" option).
        new NEIRegisterHandlerInfosEvent().registerHandlerInfo(createHandlerInfo());
    }

    @SubscribeEvent
    public void registerHandlerInfo(NEIRegisterHandlerInfosEvent event) {
        event.registerHandlerInfo(createHandlerInfo());
    }

    private HandlerInfo createHandlerInfo() {
        return new HandlerInfo.Builder(MTRecipeHandler.class, "Advanced Solar Panels", "AdvancedSolarPanel")
            .setDisplayStack(AdvancedSolarPanel.itemMolecularTransformer)
            .setWidth(167)
            .setHeight(67)
            .setMultipleWidgetsAllowed(true)
            .build();
    }

    public String getName() {
        return "Advanced Solar Panels";
    }

    public String getVersion() {
        return "v1.0";
    }
}
