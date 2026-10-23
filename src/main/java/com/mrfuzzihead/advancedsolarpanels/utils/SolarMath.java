package com.mrfuzzihead.advancedsolarpanels.utils;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenDesert;

/** IC2-compatible sun and sky-light predicates, shared by panels and helmets. */
public final class SolarMath {

    /** IC2 solar generators require at least this top-of-world sky light value. */
    public static final int MIN_SKY_LIGHT = 5;
    public static final double DAWN_DUSK_FACTOR = 1.0D / 4.0D;
    public static final double NIGHT_FACTOR = 1.0D / 8.0D;
    public static final double RAIN_DAY_FACTOR = 1.0D / 4.0D;
    public static final double RAIN_DAWN_DUSK_FACTOR = 1.0D / 8.0D;
    public static final double THUNDER_DAY_FACTOR = 1.0D / 8.0D;

    private SolarMath() {}

    /** Top-of-world sky light used by IC2's solar generator (includes time of day and weather). */
    public static int skyLight(World world, int x, int z) {
        return world.getBlockLightValue(x, 255, z);
    }

    /** Time-of-day sky light with rain/thunder removed, used to choose the graceful factor. */
    public static int clearSkyLight(World world) {
        float angle = world.getCelestialAngle(1.0F);
        float brightness = 1.0F - (MathHelper.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
        brightness = Math.max(0.0F, Math.min(1.0F, brightness));
        brightness = 1.0F - brightness;
        int subtracted = (int) ((1.0F - brightness) * 11.0F);
        return 15 - subtracted;
    }

    /** IC2's TileEntitySolarGenerator.isSunVisible, using the entity's own coordinates. */
    public static boolean isSunVisible(World world, int x, int y, int z) {
        return isSunVisible(
            skyLight(world, x, z),
            world.provider.hasNoSky,
            world.canBlockSeeTheSky(x, y + 1, z),
            world.isRaining(),
            world.isThundering(),
            world.getWorldChunkManager()
                .getBiomeGenAt(x, z) instanceof BiomeGenDesert);
    }

    /** Pure predicate for tests and callers that already have the individual conditions. */
    public static boolean isSunVisible(int skyLight, boolean hasNoSky, boolean canSeeSky, boolean raining,
        boolean thundering, boolean desert) {
        return skyLight >= MIN_SKY_LIGHT && !hasNoSky && canSeeSky && (desert || (!raining && !thundering));
    }

    /**
     * ASP's graceful light/weather scaling. Blocked sky or a no-sky dimension is still 0.
     */
    public static double generationFactor(World world, int x, int y, int z) {
        return generationFactor(
            clearSkyLight(world),
            world.provider.hasNoSky,
            world.canBlockSeeTheSky(x, y + 1, z),
            world.isRaining(),
            world.isThundering(),
            world.getWorldChunkManager()
                .getBiomeGenAt(x, z) instanceof BiomeGenDesert);
    }

    /**
     * Graceful ASP factors. Clear weather is 100% day, 1/4 dawn/dusk, 1/8 night. Rain is
     * 1/4 day, 1/8 dawn/dusk, 0 night. Thunder is 1/8 day and 0 otherwise. Deserts ignore weather.
     */
    public static double generationFactor(int clearSkyLight, boolean hasNoSky, boolean canSeeSky, boolean raining,
        boolean thundering, boolean desert) {
        if (hasNoSky || !canSeeSky) {
            return 0.0D;
        }

        int phase = clearSkyLight >= 12 ? 0 : clearSkyLight >= MIN_SKY_LIGHT ? 1 : 2;
        if (thundering && !desert) {
            return phase == 0 ? THUNDER_DAY_FACTOR : 0.0D;
        }
        if (raining && !desert) {
            if (phase == 0) {
                return RAIN_DAY_FACTOR;
            }
            if (phase == 1) {
                return RAIN_DAWN_DUSK_FACTOR;
            }
            return 0.0D;
        }

        if (phase == 0) {
            return 1.0D;
        }
        if (phase == 1) {
            return DAWN_DUSK_FACTOR;
        }
        return NIGHT_FACTOR;
    }
}
