// Default package — required to access obfuscated Minecraft runtime types.
// vp (GuiScreen), abp (GuiButton), and nl (FontRenderer) all live in the
// default package; named-package Java code cannot reference them.

import com.rorysmod.excavation.config.ModConfig;

/**
 * In-game settings screen for Rory's Excavation.
 *
 * Extends {@code vp} (GuiScreen). All five configurable values are shown as
 * interactive controls. Changes are held in local edit fields and committed to
 * {@link ModConfig} (and flushed to rorys-excavation.cfg) only when the screen
 * is closed — either by pressing Done, pressing ESC, or any other dismissal.
 * The new values take effect on the very next game tick with no restart.
 *
 * <h3>Opening</h3>
 * Triggered by {@code ExcavationHandler} when {@code openConfigKey} (default F12,
 * LWJGL code 88) is pressed while in-game with no GUI open.
 *
 * <h3>Obfuscated runtime mappings used (confirmed by javap on minecraft-1.2.5-client.jar)</h3>
 * <pre>
 *   vp                                    = GuiScreen  (extends oo)
 *   vp.p  : net.minecraft.client.Minecraft = mc
 *   vp.q  : int                            = width
 *   vp.r  : int                            = height
 *   vp.s  : java.util.List                 = buttonList  (protected)
 *   vp.u  : nl                             = fontRenderer (protected)
 *   vp.a(net.minecraft.client.Minecraft, int, int) : void  = initGui
 *   vp.a(abp) : void                       = actionPerformed  (protected)
 *   vp.a(int, int, float) : void           = drawScreen
 *   vp.a(char, int) : void                 = keyTyped  (protected)
 *   vp.e() : void                          = onGuiClosed  (confirmed: Minecraft.a(vp)V offset 22)
 *   vp.d(int) : void                       = drawBackground
 *   vp.b() : boolean                       = doesGuiPauseGame
 *   abp                                    = GuiButton  (extends oo)
 *   abp(int, int, int, int, int, String)   = GuiButton(id, x, y, width, height, text)
 *   abp.f  : int                            = id  (confirmed: 6-arg constructor stores p1 → f)
 *   abp.a  : int                            = width  (confirmed: constructor stores p4 → a)
 *   abp.e  : String                         = displayString  (public)
 *   abp.h  : boolean                        = enabled  (public)
 *   nl.a(String, int, int, int) : int      = drawString(text, x, y, color) → end-x
 *   nl.a(String) : int                     = getStringWidth(text)
 *   net.minecraft.client.Minecraft.a(vp) : void = displayGuiScreen(GuiScreen)
 *   net.minecraft.client.Minecraft.s : vp  = currentScreen  (public)
 * </pre>
 */
public class GuiExcavationConfig extends vp {

    // ── Button IDs ────────────────────────────────────────────────────────────
    private static final int BTN_ENABLE   = 0;
    private static final int BTN_ACTKEY   = 1;
    private static final int BTN_BLK_DEC  = 2;
    private static final int BTN_BLK_INC  = 3;
    private static final int BTN_DAMAGE   = 4;
    private static final int BTN_OPENKEY  = 5;
    private static final int BTN_DONE     = 6;

    private final ModConfig config;

    // ── Edited values — uncommitted until Done / screen close ─────────────────
    private boolean editEnable;
    private int     editActKey;
    private int     editMaxBlocks;
    private boolean editDamage;
    private int     editOpenKey;

    /**
     * Which button is currently in key-capture mode, or -1 if none.
     * When ≥ 0, the next key press (excluding ESC) is recorded as the new binding.
     */
    private int capturingForBtn = -1;

    public GuiExcavationConfig(ModConfig config) {
        this.config        = config;
        this.editEnable    = config.isExcavationEnabled();
        this.editActKey    = config.getActivationKeyCode();
        this.editMaxBlocks = config.getMaxBlocks();
        this.editDamage    = config.isDamagePerBlock();
        this.editOpenKey   = config.getOpenConfigKey();
    }

    // ── initGui(Minecraft, width, height) — vp.a(Minecraft,II)V ──────────────
    // Called by Minecraft after displayGuiScreen sets mc / width / height.
    @Override
    public void a(net.minecraft.client.Minecraft mc, int width, int height) {
        super.a(mc, width, height);
        rebuildButtons();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void rebuildButtons() {
        this.s.clear();
        int cx = this.q / 2;
        int y  = this.r / 2 - 80;

        // Row 0 — Enable Excavation toggle
        this.s.add(new abp(BTN_ENABLE, cx - 100, y, 200, 20,
                "Enable Excavation: " + editEnable));

        // Row 1 — Activation Key capture
        y += 24;
        String actLabel = (capturingForBtn == BTN_ACTKEY)
                ? "> Press a key <"
                : "Activation Key: " + keyName(editActKey);
        this.s.add(new abp(BTN_ACTKEY, cx - 100, y, 200, 20, actLabel));

        // Row 2 — Max Blocks  <  [label drawn in drawScreen]  >
        y += 24;
        this.s.add(new abp(BTN_BLK_DEC, cx - 100, y, 40, 20, "<"));
        this.s.add(new abp(BTN_BLK_INC, cx + 60,  y, 40, 20, ">"));

        // Row 3 — Damage Mode toggle
        y += 24;
        String dmgLabel = "Damage Mode: " + (editDamage ? "Per Block" : "Per Chain");
        this.s.add(new abp(BTN_DAMAGE, cx - 100, y, 200, 20, dmgLabel));

        // Row 4 — Open Config Key capture
        y += 24;
        String openKeyLabel = (capturingForBtn == BTN_OPENKEY)
                ? "> Press a key <"
                : "Open Config Key: " + keyName(editOpenKey);
        this.s.add(new abp(BTN_OPENKEY, cx - 100, y, 200, 20, openKeyLabel));

        // Row 5 (gap + 12) — Done
        y += 36;
        this.s.add(new abp(BTN_DONE, cx - 100, y, 200, 20, "Done"));
    }

    // ── actionPerformed(GuiButton) — vp.a(Labp;)V  (protected) ───────────────
    @Override
    protected void a(abp btn) {
        switch (btn.f) {  // btn.f = id (confirmed: 6-arg ctor stores first param in f, not a)
            case BTN_ENABLE:
                editEnable = !editEnable;
                break;
            case BTN_ACTKEY:
                capturingForBtn = BTN_ACTKEY;
                break;
            case BTN_BLK_DEC:
                editMaxBlocks = Math.max(1, editMaxBlocks - 8);
                break;
            case BTN_BLK_INC:
                editMaxBlocks = Math.min(512, editMaxBlocks + 8);
                break;
            case BTN_DAMAGE:
                editDamage = !editDamage;
                break;
            case BTN_OPENKEY:
                capturingForBtn = BTN_OPENKEY;
                break;
            case BTN_DONE:
                // displayGuiScreen(null) → triggers onGuiClosed() → save
                this.p.a((vp) null);
                return;
        }
        rebuildButtons();
    }

    // ── keyTyped(char, int) — vp.a(CI)V  (protected) ─────────────────────────
    // Called by Minecraft for every key press while this screen is showing.
    @Override
    protected void a(char c, int keyCode) {
        if (capturingForBtn >= 0) {
            if (keyCode == 1) {
                // ESC — cancel key capture without changing the binding
                capturingForBtn = -1;
            } else if (keyCode != 0) {
                if (capturingForBtn == BTN_ACTKEY) {
                    editActKey  = keyCode;
                } else if (capturingForBtn == BTN_OPENKEY) {
                    editOpenKey = keyCode;
                }
                capturingForBtn = -1;
            }
            rebuildButtons();
        } else {
            if (keyCode == 1) {
                // ESC while not capturing — close screen (triggers save via onGuiClosed)
                this.p.a((vp) null);
            }
        }
    }

    // ── onGuiClosed — vp.e()V ─────────────────────────────────────────────────
    // Called by Minecraft.displayGuiScreen whenever the current screen is replaced
    // or dismissed. Commits edited values and persists them to rorys-excavation.cfg.
    // ExcavationHandler reads config each tick, so changes apply immediately.
    // Confirmed: Minecraft.a(vp)V offset 22 calls invokevirtual vp.e:()V on old screen.
    // vp.j()V is the KEYBOARD EVENT DISPATCHER — do NOT override it.
    @Override
    public void e() {
        config.setExcavationEnabled(editEnable);
        config.setActivationKeyCode(editActKey);
        config.setMaxBlocks(editMaxBlocks);
        config.setDamagePerBlock(editDamage);
        config.setOpenConfigKey(editOpenKey);
        config.save();
    }

    // ── drawScreen(mouseX, mouseY, partialTick) — vp.a(IIF)V ─────────────────
    @Override
    public void a(int mouseX, int mouseY, float partialTick) {
        // Dark background overlay — vp.d(int) = drawBackground(tint)
        d(0);

        int cx = this.q / 2;

        // Title
        String title = "Rory's Excavation Settings";
        // nl.a(String)I = getStringWidth (confirmed by javap)
        int titleX = cx - this.u.a(title) / 2;
        // nl.a(String,int,int,int)I = drawString(text,x,y,color) → end-x (confirmed)
        this.u.a(title, titleX, this.r / 2 - 108, 0xFFFFFF);

        // Max Blocks label — centred in the gap between < (ends cx-60) and > (starts cx+60)
        // "Max Blocks: " + value, drawn vertically centred in the button row
        String maxLabel = "Max Blocks: " + editMaxBlocks;
        int maxLabelX = cx - this.u.a(maxLabel) / 2;
        this.u.a(maxLabel, maxLabelX, this.r / 2 - 26, 0xE0E0E0);

        // Key-capture hint
        if (capturingForBtn >= 0) {
            String hint = "Press any key to bind  |  ESC to cancel";
            int hintX = cx - this.u.a(hint) / 2;
            this.u.a(hint, hintX, this.r / 2 + 80, 0xFFFF55);
        }

        // Draw all buttons (iterates this.s, calls abp.a(mc,mouseX,mouseY))
        super.a(mouseX, mouseY, partialTick);
    }

    // ── doesGuiPauseGame — vp.b()Z ────────────────────────────────────────────
    // Return false: the game world continues to tick while the config screen is open.
    @Override
    public boolean b() {
        return false;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable name for the given LWJGL key code.
     * Uses Keyboard.getKeyName() which is confirmed present in LWJGL 2.x.
     */
    private static String keyName(int code) {
        String name = org.lwjgl.input.Keyboard.getKeyName(code);
        return (name != null && !name.isEmpty()) ? name + " (" + code + ")" : "? (" + code + ")";
    }
}
