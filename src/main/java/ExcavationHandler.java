// Default package — required to access obfuscated Minecraft runtime types.
// Named-package Java code cannot reference default-package classes; all classes
// that touch xd (World), vq (EntityClientPlayerMP), or pl (MovingObjectPosition)
// must live here.

import com.rorysmod.excavation.config.ModConfig;
import com.rorysmod.excavation.feature.ExcavationDetector;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import org.lwjgl.input.Keyboard;

import java.util.EnumSet;

/**
 * GAME-tick handler for block-break detection, BFS search, and single-block excavation.
 *
 * Polls {@code Minecraft.objectMouseOver} (runtime field: {@code mc.z}, type {@code pl})
 * each tick. When a tracked block transitions to air:
 * <ol>
 *   <li>Always: runs BFS to count connected matching blocks and reports the count.</li>
 *   <li>Only when the configured activation key is held: breaks exactly one extra
 *       connected block (the first BFS result) via {@code World.setBlockWithNotify}.</li>
 * </ol>
 *
 * <h3>Obfuscated runtime names used (Minecraft 1.2.5 / Forge 3.4.9.171):</h3>
 * <pre>
 *   net.minecraft.client.Minecraft.h  → thePlayer          (vq)
 *   net.minecraft.client.Minecraft.f  → theWorld           (xd)
 *   net.minecraft.client.Minecraft.w  → ingameGUI          (aiy = GuiIngame)
 *   net.minecraft.client.Minecraft.z  → objectMouseOver    (pl)
 *   aiy.a(String)                     → printChatMessage   (confirmed: adds nt to chat List)
 *   pl.g                              → entityHit          (nn)  — null when targeting a block
 *   pl.b / pl.c / pl.d               → blockX / blockY / blockZ
 *   xd.a(int,int,int)                → getBlockId          (confirmed: delegates to ack.a:(III)I)
 *   xd.e(int,int,int)                → getBlockMetadata    (confirmed: delegates to ack.c:(III)I)
 *   xd.g(int,int,int,int)            → setBlockWithNotify  (confirmed: calls d(IIII)Z then h(IIII)V
 *                                       which issues k(III)V + j(IIII)V neighbor notifications)
 * </pre>
 */
public class ExcavationHandler implements ITickHandler {

    private final ModConfig config;

    /** Position and ID of the block the player was targeting last tick. */
    private int prevX;
    private int prevY;
    private int prevZ;
    private int prevBlockId;
    private int prevMeta;

    public ExcavationHandler(ModConfig config) {
        this.config = config;
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        if (!config.isExcavationEnabled()) {
            return;
        }

        net.minecraft.client.Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc == null || mc.h == null || mc.f == null) {
            clearPrev();
            return;
        }

        // ── Step 1: check whether the last-tracked block has become air ──────────
        //
        // Done BEFORE reading objectMouseOver. When a block breaks, objectMouseOver
        // immediately snaps to whatever is behind it — so we cannot rely on it still
        // pointing at the broken position. Instead we query the world directly.
        //
        // xd.a(x,y,z) = World.getBlockId (confirmed)
        if (prevBlockId != 0 && mc.f.a(prevX, prevY, prevZ) == 0) {
            ExcavationDetector.onBlockBroken(prevBlockId, prevMeta, prevX, prevY, prevZ);

            // mc.w = ingameGUI (aiy = GuiIngame); aiy.a(String) = printChatMessage.
            if (mc.w != null) {
                mc.w.a("[RorysExcavation] Broken id=" + prevBlockId
                        + " meta=" + prevMeta
                        + " pos=(" + prevX + "," + prevY + "," + prevZ + ")");
            }

            // Capture world reference for use in anonymous inner classes.
            // xd.a = getBlockId, xd.e = getBlockMetadata (both confirmed).
            // xd.g = setBlockWithNotify (confirmed: writes block then notifies neighbors).
            final xd theWorld = mc.f;

            ExcavationDetector.WorldReader reader = new ExcavationDetector.WorldReader() {
                @Override
                public int getBlockId(int x, int y, int z) {
                    return theWorld.a(x, y, z);
                }
                @Override
                public int getBlockMeta(int x, int y, int z) {
                    return theWorld.e(x, y, z);
                }
            };

            // ── Always: BFS count for debug reporting ─────────────────────────
            int connected = ExcavationDetector.bfsConnectedBlocks(
                    reader, prevBlockId, prevMeta, prevX, prevY, prevZ,
                    config.getMaxBlocks());

            if (mc.w != null) {
                mc.w.a("[RorysExcavation] Found " + connected
                        + " connected blocks for id=" + prevBlockId
                        + " meta=" + prevMeta);
            }

            // ── Activation key gate ───────────────────────────────────────────
            // Keyboard.isKeyDown(int) is the confirmed LWJGL 2.x API for this version.
            // Key code comes from config (default: Keyboard.KEY_GRAVE = 41).
            boolean keyHeld = Keyboard.isKeyDown(config.getActivationKeyCode());

            if (keyHeld && connected > 0) {
                // Find and break exactly one extra connected matching block.
                int[] target = ExcavationDetector.bfsFirstConnectedBlock(
                        reader, prevBlockId, prevMeta, prevX, prevY, prevZ);

                if (target != null) {
                    ExcavationDetector.WorldWriter writer = new ExcavationDetector.WorldWriter() {
                        @Override
                        public void setBlock(int x, int y, int z, int blockId) {
                            // xd.g(x,y,z,blockId) = World.setBlockWithNotify (confirmed).
                            // Returns boolean (ignored — we don't need the dirty flag here).
                            theWorld.g(x, y, z, blockId);
                        }
                    };

                    ExcavationDetector.removeBlock(writer, target[0], target[1], target[2]);

                    if (mc.w != null) {
                        mc.w.a("[RorysExcavation] Excavated 1 extra block"
                                + " id=" + prevBlockId
                                + " meta=" + prevMeta
                                + " pos=(" + target[0] + "," + target[1] + "," + target[2] + ")");
                    }
                }
            }

            clearPrev();
        }

        // ── Step 2: update tracking from current objectMouseOver ─────────────────
        //
        // mc.z = objectMouseOver (pl = MovingObjectPosition).
        // pl.g = entityHit (nn); null when the target is a block, non-null for entities.
        pl mop = mc.z;
        if (mop == null || mop.g != null) {
            // MISS or entity targeted — keep prevBlockId so we can still detect
            // a break if the block disappears on the very next tick.
            return;
        }

        int x = mop.b;
        int y = mop.c;
        int z = mop.d;

        // xd.a(x,y,z) = World.getBlockId       (confirmed: delegates to ack.a)
        // xd.e(x,y,z) = World.getBlockMetadata (confirmed: delegates to ack.c)
        // xd.d(x,y,z) = World.getBlockLightValue — NOT metadata (returns 255 in full daylight)
        int blockId = mc.f.a(x, y, z);

        if (blockId != 0) {
            prevX       = x;
            prevY       = y;
            prevZ       = z;
            prevBlockId = blockId;
            prevMeta    = mc.f.e(x, y, z);
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {}

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.GAME);
    }

    @Override
    public String getLabel() {
        return "RorysExcavation_ExcavationHandler";
    }

    private void clearPrev() {
        prevBlockId = 0;
        prevMeta    = 0;
        prevX       = 0;
        prevY       = 0;
        prevZ       = 0;
    }
}
