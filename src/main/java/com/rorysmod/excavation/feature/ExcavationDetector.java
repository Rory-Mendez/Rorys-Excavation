package com.rorysmod.excavation.feature;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pure block-break detection and BFS traversal logic.
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

    private static final int[][] FACES = {
        { 1, 0, 0}, {-1, 0, 0},
        { 0, 1, 0}, { 0,-1, 0},
        { 0, 0, 1}, { 0, 0,-1}
    };

    private ExcavationDetector() {}

    /**
     * Minimal world-query interface so BFS logic never touches obfuscated types.
     * Implemented by ExcavationHandler using mc.f.a / mc.f.e.
     */
    public interface WorldReader {
        int getBlockId(int x, int y, int z);
        int getBlockMeta(int x, int y, int z);
    }

    /**
     * Called when a block disappears at the position the player was targeting.
     */
    public static void onBlockBroken(int blockId, int meta, int x, int y, int z) {
        LOG.info(PREFIX + " Block broken:"
                + " id=" + blockId
                + " meta=" + meta
                + " pos=(" + x + "," + y + "," + z + ")");
    }

    /**
     * BFS over face-adjacent blocks that share the same block ID and metadata as
     * the broken block. The broken block's position itself is excluded from the
     * search (it is now air). Returns the total count of connected matching blocks
     * found, capped at {@code maxBlocks}.
     *
     * @param world      Thin query interface over the live Minecraft world.
     * @param brokenId   Block ID that was broken (never 0).
     * @param brokenMeta Metadata of the broken block.
     * @param startX     World X of the broken block.
     * @param startY     World Y of the broken block.
     * @param startZ     World Z of the broken block.
     * @param maxBlocks  Upper limit on how many connected blocks to count.
     * @return Number of connected matching blocks found (0 – maxBlocks).
     */
    public static int bfsConnectedBlocks(
            WorldReader world,
            int brokenId, int brokenMeta,
            int startX, int startY, int startZ,
            int maxBlocks) {

        Set<Long> visited = new HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>();

        // Mark the broken position visited so we never re-enqueue it.
        visited.add(posKey(startX, startY, startZ));

        // Seed the queue with the 6 immediate neighbors that already match.
        for (int[] face : FACES) {
            int nx = startX + face[0];
            int ny = startY + face[1];
            int nz = startZ + face[2];
            if (world.getBlockId(nx, ny, nz) == brokenId
                    && world.getBlockMeta(nx, ny, nz) == brokenMeta) {
                long key = posKey(nx, ny, nz);
                if (visited.add(key)) {
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }

        int count = 0;
        while (!queue.isEmpty() && count < maxBlocks) {
            int[] pos = queue.poll();
            count++;

            for (int[] face : FACES) {
                int nx = pos[0] + face[0];
                int ny = pos[1] + face[1];
                int nz = pos[2] + face[2];
                if (world.getBlockId(nx, ny, nz) == brokenId
                        && world.getBlockMeta(nx, ny, nz) == brokenMeta) {
                    long key = posKey(nx, ny, nz);
                    if (visited.add(key)) {
                        queue.add(new int[]{nx, ny, nz});
                    }
                }
            }
        }

        return count;
    }

    // Encodes a world coordinate triple as a unique long.
    // X and Z range ±30 000 000 (26 bits each); Y is 0–127 (7 bits).
    // Layout: bits 59–34 = x+30M, bits 33–27 = y, bits 26–0 = z+30M.
    private static long posKey(int x, int y, int z) {
        return ((long)(x + 30_000_000)) << 34 | ((long)y) << 27 | (long)(z + 30_000_000);
    }
}
