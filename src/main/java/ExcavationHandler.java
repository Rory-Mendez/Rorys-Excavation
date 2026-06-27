// Default package — required to access obfuscated Minecraft runtime types.
// Named-package Java code cannot reference default-package classes; all classes
// that touch xd (World), vq (EntityClientPlayerMP), or pl (MovingObjectPosition)
// must live here.

import com.rorysmod.excavation.config.ModConfig;
import com.rorysmod.excavation.feature.ExcavationDetector;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import java.util.EnumSet;

/**
 * GAME-tick handler for block-break detection.
 *
 * Polls {@code Minecraft.objectMouseOver} (runtime field: {@code mc.z}, type {@code pl})
 * each tick. Tracks the block ID at the targeted position. When it transitions from
 * non-air to air, delegates to {@link ExcavationDetector#onBlockBroken}.
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
 *   xd.a(int,int,int)                → getBlockId          (confirmed: delegates to ack.a)
 *   xd.d(int,int,int)                → getBlockLightValue  (NOT metadata — returns 255 in full daylight)
 *   xd.e(int,int,int)                → getBlockMetadata    (confirmed: delegates to ack.c)
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

            // ── Neighbor scan ─────────────────────────────────────────────────
            // Read block ID and metadata at the 6 orthogonal face-adjacent positions.
            // +X, -X, +Y, -Y, +Z, -Z — order matches the arrays passed to the feature class.
            // xd.a = getBlockId, xd.e = getBlockMetadata (both confirmed).
            int[] nIds  = new int[6];
            int[] nMeta = new int[6];
            int[] offX  = { 1, -1,  0,  0,  0,  0 };
            int[] offY  = { 0,  0,  1, -1,  0,  0 };
            int[] offZ  = { 0,  0,  0,  0,  1, -1 };
            for (int i = 0; i < 6; i++) {
                int nx = prevX + offX[i];
                int ny = prevY + offY[i];
                int nz = prevZ + offZ[i];
                nIds[i]  = mc.f.a(nx, ny, nz);
                nMeta[i] = mc.f.e(nx, ny, nz);
            }

            int matchCount = ExcavationDetector.countMatchingNeighbors(
                    prevBlockId, prevMeta, nIds, nMeta);

            if (mc.w != null) {
                mc.w.a("[RorysExcavation] Found " + matchCount
                        + " matching neighbors for id=" + prevBlockId
                        + " meta=" + prevMeta);
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
