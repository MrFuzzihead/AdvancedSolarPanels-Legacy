package com.mrfuzzihead.advancedsolarpanels.common.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.mrfuzzihead.advancedsolarpanels.AdvancedSolarPanel;

/**
 * Typed, validated view of the mod configuration.
 *
 * <p>
 * Generation values are the 100% clear-day rate. Dusk, night, rain and thunder are derived from that rate; the old
 * {@code *GenNight} keys are intentionally ignored.
 */
public final class ASPConfig {

    public final Configuration configuration;

    public final int advGenDay;
    public final int advStorage;
    public final int advOutput;
    public final int hGenDay;
    public final int hStorage;
    public final int hOutput;
    public final int uhGenDay;
    public final int uhStorage;
    public final int uhOutput;
    public final int qpGenDay;
    public final int qpStorage;
    public final int qpOutput;
    public final int qgProduction;
    public final int qgMaxPacketSize;

    public final boolean disableAdvancedSolarHelmetRecipe;
    public final boolean disableHybridSolarHelmetRecipe;
    public final boolean disableUltimateSolarHelmetRecipe;
    public final boolean disableAdvancedSolarPanelRecipe;
    public final boolean disableHybridSolarPanelRecipe;
    public final boolean disableUltimateSolarPanelRecipe;
    public final boolean disableQuantumSolarPanelRecipe;
    public final boolean disableMolecularTransformerRecipe;
    public final boolean disableDoubleSlabRecipe;
    public final boolean enableSimpleAdvancedSolarPanelRecipes;
    public final boolean enableHardRecipes;
    public final boolean solarHelmetLegacyCharging;

    private ASPConfig(Configuration configuration) {
        this.configuration = configuration;
        this.advGenDay = clampedInt(configuration, "general", "AdvancedSPGenDay", 8, 0);
        this.advStorage = clampedInt(configuration, "general", "AdvancedSPStorage", 32000, 1);
        this.advOutput = clampedInt(configuration, "general", "AdvancedSPOutput", 32, 1);
        this.hGenDay = clampedInt(configuration, "general", "HybrydSPGenDay", 64, 0);
        this.hStorage = clampedInt(configuration, "general", "HybrydSPStorage", 100000, 1);
        this.hOutput = clampedInt(configuration, "general", "HybrydSPOutput", 128, 1);
        this.uhGenDay = clampedInt(configuration, "general", "UltimateHSPGenDay", 512, 0);
        this.uhStorage = clampedInt(configuration, "general", "UltimateHSPStorage", 1000000, 1);
        this.uhOutput = clampedInt(configuration, "general", "UltimateHSPOutput", 512, 1);
        this.qpGenDay = clampedInt(configuration, "general", "QuantumSPGenDay", 4096, 0);
        this.qpStorage = clampedInt(configuration, "general", "QuantumSPStorage", 10000000, 1);
        this.qpOutput = clampedInt(configuration, "general", "QuantumSPOutput", 8192, 1);
        this.qgProduction = clampedInt(configuration, "quantum generator", "quantumGeneratorDefaultProduction", 512, 1);
        this.qgMaxPacketSize = clampedInt(
            configuration,
            "quantum generator",
            "quantumGeneratorDefaultPacketSize",
            512,
            1);

        this.disableAdvancedSolarHelmetRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable Advanced Solar Helmet recipe",
            false);
        this.disableHybridSolarHelmetRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable Hybrid Solar Helmet recipe",
            false);
        this.disableUltimateSolarHelmetRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable Ultimate Solar Helmet recipe",
            false);
        this.disableAdvancedSolarPanelRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable AdvancedSolarPanel recipe",
            false);
        this.disableHybridSolarPanelRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable HybridSolarPanel recipe",
            false);
        this.disableUltimateSolarPanelRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable UltimateSolarPanel recipe",
            false);
        this.disableQuantumSolarPanelRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable QuantumSolarPanel recipe",
            false);
        this.disableMolecularTransformerRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable MolecularTransformer recipe",
            false);
        this.disableDoubleSlabRecipe = booleanValue(
            configuration,
            "recipes settings",
            "Disable DoubleSlab recipe",
            false);
        this.enableSimpleAdvancedSolarPanelRecipes = booleanValue(
            configuration,
            "recipes settings",
            "Enable simple Advanced Solar Panel recipe",
            false);
        this.enableHardRecipes = booleanValue(configuration, "recipes settings", "Enable hard recipes", true);
        this.solarHelmetLegacyCharging = booleanValue(configuration, "general", "solarHelmetLegacyCharging", false);

        addComments(configuration);
        warnAboutLegacyNightKeys(configuration);
    }

    public static ASPConfig load(File file) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            return new ASPConfig(configuration);
        } catch (RuntimeException exception) {
            AdvancedSolarPanel.addLog("Error parsing config; using defaults where possible.");
            throw exception;
        } finally {
            configuration.save();
        }
    }

    private static int clampedInt(Configuration configuration, String category, String key, int defaultValue,
        int minimum) {
        int value = configuration.get(category, key, defaultValue)
            .getInt(defaultValue);
        if (value < minimum) {
            AdvancedSolarPanel
                .addLog("Config " + key + "=" + value + " is below the minimum " + minimum + "; clamping.");
            return minimum;
        }
        return value;
    }

    private static boolean booleanValue(Configuration configuration, String category, String key,
        boolean defaultValue) {
        return configuration.get(category, key, defaultValue)
            .getBoolean(defaultValue);
    }

    private static void addComments(Configuration configuration) {
        configuration.getCategory("general")
            .setComment(
                "Generation values are the 100% clear-day rate. Dusk/dawn, night, rain and thunder are scaled "
                    + "automatically. The old *GenNight options are ignored.");
        configuration.getCategory("quantum generator")
            .setComment("Admin generator limits. Redstone disables output; GUI buttons adjust these values.");
        configuration.getCategory("recipes settings")
            .setComment("Recipe toggles are read after all mods have loaded so cross-mod recipes are visible.");
    }

    private static void warnAboutLegacyNightKeys(Configuration configuration) {
        String[] legacyKeys = { "AdvancedSPGenNight", "HybrydSPGenNight", "UltimateHSPGenNight", "QuantumSPGenNight" };
        for (String key : legacyKeys) {
            if (configuration.getCategory("general")
                .containsKey(key)) {
                AdvancedSolarPanel.addLog("Config " + key + " is obsolete and ignored; *GenDay is the 100% rate.");
            }
        }
    }
}
