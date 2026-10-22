package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.SolarMath;

class SolarMathTest {

    @Test
    void requiresSkyLightAboveFour() {
        assertFalse(SolarMath.isSunVisible(4, false, true, false, false, false));
        assertTrue(SolarMath.isSunVisible(5, false, true, false, false, false));
        assertTrue(SolarMath.isSunVisible(15, false, true, false, false, false));
    }

    @Test
    void rejectsBlockedOrSkyLessWorlds() {
        assertFalse(SolarMath.isSunVisible(15, true, true, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, false, false, false, false));
    }

    @Test
    void gracefulGenerationFactors() {
        assertEquals(0.0D, SolarMath.generationFactor(15, true, true, false, false, false), 0.0D);
        assertEquals(0.0D, SolarMath.generationFactor(15, false, false, false, false, false), 0.0D);
        assertEquals(1.0D, SolarMath.generationFactor(15, false, true, false, false, false), 0.0D);
        assertEquals(2.0D / 3.0D, SolarMath.generationFactor(11, false, true, false, false, false), 0.0D);
        assertEquals(1.0D / 3.0D, SolarMath.generationFactor(4, false, true, false, false, false), 0.0D);
        assertEquals(1.0D / 3.0D, SolarMath.generationFactor(15, false, true, true, false, false), 0.0D);
        assertEquals(1.0D, SolarMath.generationFactor(15, false, true, true, false, true), 0.0D);
    }

    @Test
    void followsIc2WeatherAndDesertRules() {
        assertFalse(SolarMath.isSunVisible(15, false, true, true, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, true, false, true, false));
        assertTrue(SolarMath.isSunVisible(15, false, true, true, false, true));
        assertTrue(SolarMath.isSunVisible(15, false, true, false, true, true));
    }
}
