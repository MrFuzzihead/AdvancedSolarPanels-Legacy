package com.mrfuzzihead.advancedsolarpanels.utils;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenDesert;

/** IC2-compatible sun and sky-light predicates, shared by panels and helmets. */
public final class SolarMath {

    /** IC2 solar generators require at least this top-of-world sky light value. */
    public static final int MIN_SKY_LIGHT = 5;

    private SolarMath() {}

    /** Top-of-world sky light used by IC2's solar generator (includes time of day and weather). */
    public static int skyLight(World world, int x, int z) {
        return world.getBlockLightValue(x, 255, z);
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
}
