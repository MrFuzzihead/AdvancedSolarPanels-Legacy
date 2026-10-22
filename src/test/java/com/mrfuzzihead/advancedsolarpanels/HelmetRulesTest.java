package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.HelmetRules;

class HelmetRulesTest {

    @Test
    void helmetTierMatchesPanelTier() {
        assertEquals(1, HelmetRules.tier(1));
        assertEquals(2, HelmetRules.tier(2));
        assertEquals(3, HelmetRules.tier(3));
        assertEquals(1, HelmetRules.tier(0));
        assertEquals(1, HelmetRules.tier(99));
    }

    @Test
    void chargeTargetMatchesIc2SolarHelmet() {
        assertEquals(2, HelmetRules.CHEST_ARMOR_SLOT);
        assertEquals(Integer.MAX_VALUE, HelmetRules.ANY_CHARGE_TIER);
        assertTrue(HelmetRules.IGNORE_TRANSFER_LIMIT);
    }

    @Test
    void onlyHybridAndUltimateProvideAir() {
        assertFalse(HelmetRules.hasAirSupply(1));
        assertTrue(HelmetRules.hasAirSupply(2));
        assertTrue(HelmetRules.hasAirSupply(3));
        assertFalse(HelmetRules.hasAirSupply(4));
    }
}
