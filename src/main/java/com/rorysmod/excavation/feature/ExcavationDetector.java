package com.rorysmod.excavation.feature;

import java.util.logging.Logger;

/**
 * Pure block-break detection logic.
 *
 * No Forge coupling, no LWJGL, no obfuscated Minecraft types.
 * All inputs are plain Java primitives or standard types.
 *
 * Called by ExcavationHandler when it detects a block transitioning
 * from non-air to air at the player's aim position.
 */
public final class ExcavationDetector {

    private static final Logger LOG = Logger.getLogger("Minecraft");
    private static final String PREFIX = "[RorysExcavation]";

    private ExcavationDetector() {}

    /**
     * Called when a block disappears at the position the player was targeting.
     *
     * @param blockId  Block ID that was present before disappearing (never 0).
     * @param meta     Block metadata at the time of break.
     * @param x        World X coordinate.
     * @param y        World Y coordinate.
     * @param z        World Z coordinate.
     */
    public static void onBlockBroken(int blockId, int meta, int x, int y, int z) {
        LOG.info(PREFIX + " Block broken:"
                + " id=" + blockId
                + " meta=" + meta
                + " pos=(" + x + "," + y + "," + z + ")");
    }
}
