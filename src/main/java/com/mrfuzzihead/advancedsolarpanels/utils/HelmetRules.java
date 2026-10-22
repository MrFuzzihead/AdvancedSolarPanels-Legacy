package com.mrfuzzihead.advancedsolarpanels.utils;

/** Pure helmet tier/feature rules, kept separate from the ItemArmor implementation for testing. */
public final class HelmetRules {

    /** IC2's solar helmet only charges the chestplate. */
    public static final int CHEST_ARMOR_SLOT = 2;
    /** IC2 charges the chestplate without a tier restriction. */
    public static final int ANY_CHARGE_TIER = Integer.MAX_VALUE;
    /** IC2 ignores the target item's transfer limit for the solar helmet. */
    public static final boolean IGNORE_TRANSFER_LIMIT = true;

    private HelmetRules() {}

    /** Advanced=1, Hybrid=2, Ultimate=3; invalid values fall back to Advanced. */
    public static int tier(int solarType) {
        return solarType >= 1 && solarType <= 3 ? solarType : 1;
    }

    public static boolean hasAirSupply(int solarType) {
        return solarType == 2 || solarType == 3;
    }
}
