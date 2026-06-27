package com.rorysmod.excavation.feature;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pure block-break detection, BFS traversal, and world-modification logic.
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

    // ─────────────────────────────────────────────────────────────────────────
    // World-access interfaces
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Minimal world-query interface so BFS logic never touches obfuscated types.
     * Implemented by ExcavationHandler using mc.f.a / mc.f.e.
     */
    public interface WorldReader {
        int getBlockId(int x, int y, int z);
        int getBlockMeta(int x, int y, int z);
    }

    /**
     * Minimal world-modification interface so block-break logic never touches
     * obfuscated types. Implemented by ExcavationHandler using mc.f.g (confirmed
     * as World.setBlockWithNotify in Minecraft 1.2.5 / Forge 3.4.9.171).
     */
    public interface WorldWriter {
        /** Set the block at (x,y,z) to the given blockId (0 = air). */
        void setBlock(int x, int y, int z, int blockId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when a block disappears at the position the player was targeting.
     */
    public static void onBlockBroken(int blockId, int meta, int x, int y, int z) {
        LOG.info(PREFIX + " Block broken:"
                + " id=" + blockId
                + " meta=" + meta
                + " pos=(" + x + "," + y + "," + z + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BFS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BFS over face-adjacent blocks that share the same block ID and metadata as
     * the broken block. Returns the total count of connected matching blocks found,
     * capped at {@code maxBlocks}. Does not modify the world.
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

        visited.add(posKey(startX, startY, startZ));
        seedQueue(world, brokenId, brokenMeta, startX, startY, startZ, visited, queue);

        int count = 0;
        while (!queue.isEmpty() && count < maxBlocks) {
            int[] pos = queue.poll();
            count++;
            seedQueue(world, brokenId, brokenMeta, pos[0], pos[1], pos[2], visited, queue);
        }

        return count;
    }

    /**
     * BFS that returns the coordinates of the first connected matching block found,
     * or {@code null} if no connected matching blocks exist. Used when we need to
     * act on exactly one block rather than count the whole set.
     *
     * @param world      Thin query interface over the live Minecraft world.
     * @param brokenId   Block ID that was broken (never 0).
     * @param brokenMeta Metadata of the broken block.
     * @param startX     World X of the broken block.
     * @param startY     World Y of the broken block.
     * @param startZ     World Z of the broken block.
     * @return int[]{x, y, z} of the first BFS-reachable matching block, or null.
     */
    public static int[] bfsFirstConnectedBlock(
            WorldReader world,
            int brokenId, int brokenMeta,
            int startX, int startY, int startZ) {

        Set<Long> visited = new HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>();

        visited.add(posKey(startX, startY, startZ));
        seedQueue(world, brokenId, brokenMeta, startX, startY, startZ, visited, queue);

        return queue.isEmpty() ? null : queue.poll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // World modification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Removes the block at (x,y,z) by setting it to air via the WorldWriter.
     * The caller is responsible for ensuring the position holds the expected block.
     */
    public static void removeBlock(WorldWriter writer, int x, int y, int z) {
        writer.setBlock(x, y, z, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void seedQueue(
            WorldReader world,
            int id, int meta,
            int cx, int cy, int cz,
            Set<Long> visited,
            Queue<int[]> queue) {
        for (int[] face : FACES) {
            int nx = cx + face[0];
            int ny = cy + face[1];
            int nz = cz + face[2];
            if (world.getBlockId(nx, ny, nz) == id
                    && world.getBlockMeta(nx, ny, nz) == meta) {
                long key = posKey(nx, ny, nz);
                if (visited.add(key)) {
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }
    }

    // Encodes a world coordinate triple as a unique long.
    // X and Z range ±30 000 000 (26 bits each); Y is 0–127 (7 bits).
    // Layout: bits 59–34 = x+30M, bits 33–27 = y, bits 26–0 = z+30M.
    private static long posKey(int x, int y, int z) {
        return ((long)(x + 30_000_000)) << 34 | ((long)y) << 27 | (long)(z + 30_000_000);
    }
}
