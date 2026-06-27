package com.rorysmod.excavation.feature;

import java.util.logging.Logger;

/**
 * Pure block-break detection and neighbor-scan logic.
 *
 * No Forge coupling, no LWJGL, no obfuscated Minecraft types.
 * All inputs are plain Java primitives or standard-library types.
 *
 * ExcavationHandler feeds this class raw int data extracted from the
 * obfuscated Minecraft runtime so this class can focus on logic only.
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

    /**
     * Counts how many of the six orthogonal neighbors share the same block ID
     * and metadata as the broken block.
     *
     * <p>Both arrays must have exactly 6 elements, in the order the caller
     * chose (convention: +X, -X, +Y, -Y, +Z, -Z). Air neighbors (id == 0)
     * will never match since the broken block ID is always non-zero.</p>
     *
     * @param brokenId    Block ID of the block that was broken.
     * @param brokenMeta  Metadata of the block that was broken.
     * @param neighborIds Block IDs for the six orthogonal neighbors.
     * @param neighborMetas Block metadata for the six orthogonal neighbors.
     * @return Number of neighbors with matching ID and metadata (0–6).
     */
    public static int countMatchingNeighbors(
            int brokenId, int brokenMeta,
            int[] neighborIds, int[] neighborMetas) {
        int count = 0;
        for (int i = 0; i < neighborIds.length; i++) {
            if (neighborIds[i] == brokenId && neighborMetas[i] == brokenMeta) {
                count++;
            }
        }
        return count;
    }
}
