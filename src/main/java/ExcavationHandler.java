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
import java.util.List;

/**
 * GAME-tick handler for block-break detection, BFS search, and vein excavation.
 *
 * Polls {@code Minecraft.objectMouseOver} (runtime field: {@code mc.z}, type {@code pl})
 * each tick. When a tracked block transitions to air:
 * <ol>
 *   <li>Always: runs BFS to count connected matching blocks and reports the count.</li>
 *   <li>Only when the configured activation key is held: collects all connected matching
 *       blocks (up to maxBlocks), spawns their drops clustered at the original broken
 *       position, and removes each via {@code World.setBlockWithNotify}.</li>
 * </ol>
 *
 * <h3>Obfuscated runtime names used (Minecraft 1.2.5 / Forge 3.4.9.171):</h3>
 * <pre>
 *   net.minecraft.client.Minecraft.h       → thePlayer          (vq)
 *   net.minecraft.client.Minecraft.f       → theWorld           (xd)
 *   net.minecraft.client.Minecraft.w       → ingameGUI          (aiy = GuiIngame)
 *   net.minecraft.client.Minecraft.z       → objectMouseOver    (pl)
 *   aiy.a(String)                          → printChatMessage
 *   pl.g                                   → entityHit (nn)  — null when targeting a block
 *   pl.b / pl.c / pl.d                     → blockX / blockY / blockZ
 *   xd.a(int,int,int)                      → getBlockId
 *   xd.e(int,int,int)                      → getBlockMetadata
 *   xd.g(int,int,int,int)                  → setBlockWithNotify
 *   xd.b                                   → loadedEntityList (List; all live non-player entities)
 *   xd.F                                   → isRemote (boolean; always false for mc.f in SSP)
 *   pb                                     → Block class
 *   pb.m                                   → Block.blocksList (static pb[], size 4096)
 *   pb.a(xd,int,int,int,int,float,int)     → dropBlockAsItemWithChance(world,x,y,z,meta,chance,fortune)
 *   fq                                     → EntityItem (item entity; extends nn)
 *   nn.d(double,double,double)             → setPosition(x,y,z); updates posX/Y/Z and AABB
 *   nn.r / nn.s / nn.t                     → motionX / motionY / motionZ (public double)
 *   acq                                    → EntityLivingBase (abstract; vq extends yw extends acq)
 *   yw.av()                                → getCurrentEquippedItem(); delegates to aak.b() = ap.a[ap.c]
 *   yw.ah()                                → getItemInUse(); returns yw.d (null unless eating/drinking)
 *   yw.aT                                  → PlayerCapabilities (type qu; public field)
 *   qu.c                                   → isCreativeMode (boolean; confirmed: addExhaustion skips when true)
 *   aan.a(int,acq)                         → damageItem(amount,EntityLivingBase); handles Unbreaking + tool break
 *   aan.e()                                → isItemStackDamageable(); yr.h() > 0 (item has max durability)
 *   aan.a                                  → stackSize (public int)
 * </pre>
 *
 * <h3>Drop clustering (v0.6.0):</h3>
 * <p>Extra excavated blocks are harvested silently: no break sound, no particles.
 * {@code Block.dropBlockAsItemWithChance} is still called for each extra block so that
 * native and modded drop tables are respected. After each call, every {@code EntityItem}
 * ({@code fq}) newly added to {@code xd.b} (loadedEntityList) is repositioned to the
 * center of the original broken block and its velocity is zeroed. This eliminates the
 * random spawn offset and the upward/sideways kick that would otherwise scatter items.</p>
 *
 * <h3>Tool durability (v0.7.0):</h3>
 * <p>{@code ItemStack.damageItem} ({@code aan.a(int, acq)}) is called on the held item
 * after the extra blocks are removed. It handles Unbreaking enchantments (random skip),
 * damage accumulation, and tool-break animation + inventory cleanup when max durability
 * is exceeded. Creative mode ({@code qu.c}) is checked by this mod before calling
 * damageItem because 1.2.5's damageItem does not perform that check internally.
 * Empty hand is guarded by a null check on {@code yw.av()}.</p>
 *
 * <p>{@code xd.F} (isRemote) is always {@code false} for {@code mc.f} in SSP:
 * confirmed by javap inspection — all xd constructors initialise F to false, and the
 * only xd subclass (je) never sets F to true. dropBlockAsItemWithChance returns early
 * when isRemote is true, so verifying F=false is critical.</p>
 */
public class ExcavationHandler implements ITickHandler {

    private final ModConfig config;

    /** Position and ID of the block the player was targeting last tick. */
    private int prevX;
    private int prevY;
    private int prevZ;
    private int prevBlockId;
    private int prevMeta;

    /**
     * Edge-detection state for the open-config key.
     * true while the key was held last tick; used to fire on press, not hold.
     */
    private boolean prevOpenKeyDown = false;

    public ExcavationHandler(ModConfig config) {
        this.config = config;
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        net.minecraft.client.Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc == null) {
            clearPrev();
            prevOpenKeyDown = false;
            return;
        }

        // ── Open-config key gate ──────────────────────────────────────────────
        // Fires on the leading edge (press, not hold) when the player is in-game
        // with no other GUI open. Runs regardless of enableExcavation so the
        // screen is always reachable. config.getOpenConfigKey() is read every tick
        // so a newly saved binding takes effect immediately without restart.
        //
        // mc.s = currentScreen (vp / GuiScreen); null when no GUI is open.
        // mc.h = thePlayer (vq); null in the main menu.
        // mc.a(vp) = displayGuiScreen(GuiScreen) (confirmed by javap).
        boolean openKeyDown = Keyboard.isKeyDown(config.getOpenConfigKey());
        if (openKeyDown && !prevOpenKeyDown && mc.h != null && mc.s == null) {
            mc.a(new GuiExcavationConfig(config));
        }
        prevOpenKeyDown = openKeyDown;

        // ── Excavation tick ───────────────────────────────────────────────────
        if (!config.isExcavationEnabled()) {
            return;
        }

        if (mc.h == null || mc.f == null) {
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

            // ── Activation key gate ───────────────────────────────────────────
            // Keyboard.isKeyDown(int) is the confirmed LWJGL 2.x API for this version.
            // Key code comes from config (default: Keyboard.KEY_GRAVE = 41).
            boolean keyHeld = Keyboard.isKeyDown(config.getActivationKeyCode());

            if (keyHeld) {
                // ── Excavation path: collect all connected blocks then remove them ─
                // bfsCollectBlocks returns positions capped at maxBlocks; the broken
                // block's own position is excluded by BFS (it is now air and does not
                // match brokenId).
                List<int[]> targets = ExcavationDetector.bfsCollectBlocks(
                        reader, prevBlockId, prevMeta, prevX, prevY, prevZ,
                        config.getMaxBlocks());

                if (!targets.isEmpty()) {
                    ExcavationDetector.WorldWriter writer = new ExcavationDetector.WorldWriter() {
                        @Override
                        public void setBlock(int x, int y, int z, int blockId) {
                            // xd.g(x,y,z,blockId) = World.setBlockWithNotify (confirmed).
                            theWorld.g(x, y, z, blockId);
                        }
                    };

                    // pb      = Block class (confirmed by javap of minecraft-1.2.5-client.jar)
                    // pb.m    = Block.blocksList (static pb[], size 4096, indexed by block ID)
                    // pb.a(xd,int,int,int,int,float,int) = Block.dropBlockAsItemWithChance (confirmed)
                    //
                    // Drop clustering strategy (v0.7.0):
                    //   1. Snapshot xd.b (loadedEntityList) size before calling the drop.
                    //   2. Call dropBlockAsItemWithChance — this runs the full native/modded
                    //      drop chain (idDropped, quantityDropped), constructs the ItemStack,
                    //      and calls the protected Block.a(xd,x,y,z,ItemStack) spawner which
                    //      applies a random [+0.15, +0.85] offset then invokes xd.a(nn) =
                    //      spawnEntityInWorld, adding the EntityItem (fq) to xd.b.
                    //   3. For every fq added to xd.b since the snapshot:
                    //      - Call nn.d(cx, cy, cz) = setPosition (updates posX/Y/Z + AABB).
                    //      - Zero nn.r/s/t (motionX/Y/Z) to prevent items from bouncing apart.
                    //   This preserves native and modded drop logic while clustering all items
                    //   tightly at the center of the original broken block.
                    //
                    // xd.b   = loadedEntityList (List; confirmed from spawnEntityInWorld bytecode)
                    // nn.d   = setPosition(DDD)  (confirmed: puts nn.o/p/q + updates AABB nn.y)
                    // nn.r/s/t = motionX/Y/Z     (confirmed: public double fields in nn)
                    //
                    // All BFS-matched extra blocks share prevBlockId and prevMeta (BFS matches
                    // by both id and meta), so prevMeta is correct for every drop call.
                    pb blockInst = (prevBlockId >= 0 && prevBlockId < pb.m.length)
                            ? pb.m[prevBlockId] : null;

                    // Center of the original broken block — all drops will be repositioned here.
                    double cx = prevX + 0.5;
                    double cy = prevY + 0.5;
                    double cz = prevZ + 0.5;

                    // ── Tool durability setup ─────────────────────────────────────
                    // yw.av()    = getCurrentEquippedItem(); delegates to aak.b() = ap.a[ap.c]
                    // yw.ah()    = getItemInUse(); returns yw.d — null unless player is eating/drinking
                    // aak.c      = currentItem (selected hotbar slot 0-8)
                    // yw.aT      = PlayerCapabilities (qu; public)
                    // qu.c       = isCreativeMode (confirmed: addExhaustion skips when true)
                    // aan.b()    = getMaxDamage(ItemStack); calls virtual yr.g(aan) — overridden by tools
                    // aan.a(I,acq) = damageItem(amount, EntityLivingBase)
                    //               handles Unbreaking (random skip) and tool breaking.
                    //               Does NOT check creative mode — must be guarded here.
                    // aan.a      = stackSize (public int); guard > 0 to avoid repeated break FX
                    //               if the tool already broke on a previous iteration.
                    boolean damagePerBlock = config.isDamagePerBlock();
                    boolean creative       = mc.h.aT.c;
                    // yw.av() = getCurrentEquippedItem via inventory: ap.a[ap.c]
                    // yw.ah() returns yw.d (itemInUse) which is null unless right-clicking to eat/drink.
                    aan firstTool          = mc.h.av();

                    // aan.b() calls virtual yr.g(aan) — overridden by tool subclasses to return max
                    // damage. Base yr.g(aan) returns 0, so non-damageable items yield canDamage=false.
                    boolean canDamage = firstTool != null && !creative && firstTool.b() > 0;

                    for (int[] pos : targets) {
                        if (blockInst != null) {
                            // Snapshot entity list size before the drop call.
                            int entityCountBefore = theWorld.b.size();

                            // Spawn drops using native block logic (handles vanilla and modded).
                            blockInst.a(theWorld, prevX, prevY, prevZ, prevMeta, 1.0f, 0);

                            // Reposition and zero velocity on every EntityItem just spawned.
                            for (int ei = entityCountBefore; ei < theWorld.b.size(); ei++) {
                                Object ent = theWorld.b.get(ei);
                                if (ent instanceof fq) {
                                    fq item = (fq) ent;
                                    // setPosition updates posX/Y/Z (nn.o/p/q) and the AABB.
                                    item.d(cx, cy, cz);
                                    item.r = 0.0; // motionX
                                    item.s = 0.0; // motionY
                                    item.t = 0.0; // motionZ
                                }
                            }
                        }
                        // Remove the extra block silently (no break sound, no particles).
                        ExcavationDetector.removeBlock(writer, pos[0], pos[1], pos[2]);

                        // Per-block durability: re-fetch via yw.av() each iteration in case
                        // setBlockWithNotify triggered an inventory update mid-loop.
                        if (damagePerBlock && !creative) {
                            aan tool = mc.h.av();
                            if (tool != null && tool.b() > 0 && tool.a > 0) {
                                tool.a(1, mc.h);
                            }
                        }
                    }

                    // Per-chain durability: damage exactly once for the whole excavation.
                    if (!damagePerBlock && canDamage && firstTool != null && firstTool.a > 0) {
                        firstTool.a(1, mc.h);
                    }

                    if (mc.w != null) {
                        mc.w.a("[RorysExcavation] Excavated " + targets.size()
                                + " extra blocks for id=" + prevBlockId
                                + " meta=" + prevMeta);
                    }
                }
            } else {
                // ── Count-only path: report but do not modify the world ───────────
                int connected = ExcavationDetector.bfsConnectedBlocks(
                        reader, prevBlockId, prevMeta, prevX, prevY, prevZ,
                        config.getMaxBlocks());

                if (mc.w != null) {
                    mc.w.a("[RorysExcavation] Found " + connected
                            + " connected blocks for id=" + prevBlockId
                            + " meta=" + prevMeta);
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
