package com.rorysmod.excavation.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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

    // All 26 neighbors of a block: faces (6) + edges (12) + corners (8).
    // Diagonal connectivity is required for ore veins that touch only at edges or corners.
    private static final int[][] NEIGHBORS;
    static {
        NEIGHBORS = new int[26][3];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        NEIGHBORS[i++] = new int[]{dx, dy, dz};
                    }
                }
            }
        }
    }

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
    // BFS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BFS over all 26 neighbors (faces, edges, corners) that share the same block
     * ID and metadata as the broken block. Returns the count of connected matching
     * blocks found, capped at {@code maxBlocks}. Does not allocate a position list
     * — use this path when no world modification is required (key not held).
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
     * BFS over all 26 neighbors (faces, edges, corners) that share the same block
     * ID and metadata as the broken block. Returns the positions of all connected
     * matching blocks found, capped at {@code maxBlocks}. Use this path when the
     * caller needs to act on each block (key held — full excavation).
     *
     * <p>The broken block's own position is excluded from the result; it was
     * already removed by the player.</p>
     *
     * @param world      Thin query interface over the live Minecraft world.
     * @param brokenId   Block ID that was broken (never 0).
     * @param brokenMeta Metadata of the broken block.
     * @param startX     World X of the broken block.
     * @param startY     World Y of the broken block.
     * @param startZ     World Z of the broken block.
     * @param maxBlocks  Upper limit on how many blocks to collect.
     * @return List of int[]{x, y, z} for each connected matching block, size 0–maxBlocks.
     */
    public static List<int[]> bfsCollectBlocks(
            WorldReader world,
            int brokenId, int brokenMeta,
            int startX, int startY, int startZ,
            int maxBlocks) {

        Set<Long> visited = new HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>();
        List<int[]> result = new ArrayList<>();

        visited.add(posKey(startX, startY, startZ));
        seedQueue(world, brokenId, brokenMeta, startX, startY, startZ, visited, queue);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            int[] pos = queue.poll();
            result.add(pos);
            seedQueue(world, brokenId, brokenMeta, pos[0], pos[1], pos[2], visited, queue);
        }

        return result;
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
        for (int[] n : NEIGHBORS) {
            int nx = cx + n[0];
            int ny = cy + n[1];
            int nz = cz + n[2];
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
