package com.rorysmod.excavation.config;

import forge.Configuration;
import forge.Property;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads, exposes, and persists all mod configuration values.
 *
 * Uses forge.Configuration (package: forge, NOT net.minecraftforge.common).
 * Confirmed present in Forge 3.4.9.171 client jar.
 *
 * Property objects are stored as fields so that in-memory edits
 * (via the in-game settings screen) can be flushed back to disk
 * by updating Property.value and calling forge.save().
 *
 * forge.Property fields confirmed by javap of forge-1.2.5-3.4.9.171-client.jar:
 *   public String name;
 *   public String value;   <- writable; updated by setters before save()
 *   public String comment;
 * forge.Configuration methods:
 *   getOrCreateBooleanProperty(String name, String category, boolean default) -> Property
 *   getOrCreateIntProperty(String name, String category, int default)         -> Property
 *   getOrCreateProperty(String name, String category, String default)         -> Property  (string variant)
 *   save() -> writes current property values to disk
 */
public class ModConfig {

    /** LWJGL key code for the grave / tilde key (default excavation activation). */
    public static final int DEFAULT_ACTIVATION_KEY  = Keyboard.KEY_GRAVE;
    public static final int DEFAULT_MAX_BLOCKS      = 64;
    /** LWJGL key code for F12 (default open-config key). */
    public static final int DEFAULT_OPEN_CONFIG_KEY = Keyboard.KEY_F12;

    /**
     * Default blacklist — block IDs that are never excavated automatically.
     *
     *   7   Bedrock          — indestructible; excavating it does nothing but wastes cycles
     *  52   Mob Spawner      — removing spawners silently is almost always unintentional
     *  54   Chest            — auto-destroying chests would lose inventory contents
     *  61   Furnace          — same risk as Chest (items inside)
     *  62   Burning Furnace  — same risk; active state means contents are present
     *  63   Sign (post)      — signs carry text data; silent removal is surprising
     *  64   Wooden Door      — doors are structural / half-block entities
     *  68   Wall Sign        — same as Sign post
     *  71   Iron Door        — same as Wooden Door
     */
    public static final String DEFAULT_BLACKLIST =
            "7,52,54,61,62,63,64,68,71";

    private final Configuration forge;

    // ── Cached in-memory values ───────────────────────────────────────────────
    private boolean excavationEnabled;
    private int     activationKeyCode;
    private int     maxBlocks;
    private boolean damagePerBlock;
    private int     openConfigKey;
    private boolean debugMessages;
    private Set<Integer> blacklistSet;

    // ── Property refs — needed to update values before save() ────────────────
    private Property enableProp;
    private Property keyProp;
    private Property maxProp;
    private Property durProp;
    private Property openKeyProp;
    private Property debugProp;
    private Property blacklistProp;

    public ModConfig(File file) {
        this.forge = new Configuration(file);
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    public void load() {
        forge.load();

        enableProp = forge.getOrCreateBooleanProperty(
                "enableExcavation",
                Configuration.CATEGORY_GENERAL,
                true
        );
        enableProp.comment = "Set to false to disable all excavation behavior.";
        excavationEnabled  = enableProp.getBoolean(true);

        keyProp = forge.getOrCreateIntProperty(
                "activationKey",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_ACTIVATION_KEY
        );
        keyProp.comment   = "LWJGL key code for the excavation activation key. "
                + "Default: " + DEFAULT_ACTIVATION_KEY + " (Grave / tilde `). "
                + "Hold this key while breaking a block to trigger excavation. "
                + "Rebindable in-game via the Rory's Excavation settings screen (F12).";
        activationKeyCode = keyProp.getInt(DEFAULT_ACTIVATION_KEY);

        maxProp = forge.getOrCreateIntProperty(
                "maxBlocks",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_MAX_BLOCKS
        );
        maxProp.comment = "Maximum number of connected blocks to break in one excavation action. "
                + "Range: 1-512. Default: " + DEFAULT_MAX_BLOCKS + ". "
                + "Adjustable in-game via the Rory's Excavation settings screen.";
        maxBlocks       = maxProp.getInt(DEFAULT_MAX_BLOCKS);

        durProp = forge.getOrCreateBooleanProperty(
                "damagePerBlock",
                Configuration.CATEGORY_GENERAL,
                false
        );
        durProp.comment = "If false (default), tool durability is deducted only once per excavation chain. "
                + "If true, durability is deducted once for every block broken. "
                + "Toggleable in-game via the Rory's Excavation settings screen.";
        damagePerBlock  = durProp.getBoolean(false);

        openKeyProp = forge.getOrCreateIntProperty(
                "openConfigKey",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_OPEN_CONFIG_KEY
        );
        openKeyProp.comment = "LWJGL key code to open the Rory's Excavation in-game settings screen. "
                + "Default: " + DEFAULT_OPEN_CONFIG_KEY + " (F12). "
                + "Press this key while in-game with no other GUI open. "
                + "Rebindable in-game from the settings screen itself.";
        openConfigKey   = openKeyProp.getInt(DEFAULT_OPEN_CONFIG_KEY);

        debugProp = forge.getOrCreateBooleanProperty(
                "debugMessages",
                Configuration.CATEGORY_GENERAL,
                false
        );
        debugProp.comment = "If true, excavation debug messages are printed to in-game chat: "
                + "block-break detection, connected-block count, and excavation totals. "
                + "Disabled by default — enable only for troubleshooting. "
                + "Toggleable in-game via the Rory's Excavation settings screen.";
        debugMessages = debugProp.getBoolean(false);

        // getOrCreateProperty(name, category, default) is the string variant of the config API.
        // Confirmed: forge.Configuration only exposes getOrCreateProperty for string values
        // (getOrCreateStringProperty does not exist in Forge 3.4.9.171).
        blacklistProp = forge.getOrCreateProperty(
                "blacklist",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_BLACKLIST
        );
        blacklistProp.comment = "Comma-separated list of block IDs that will never be excavated "
                + "automatically. Manual breaking always works normally. "
                + "Invalid or out-of-range values are silently ignored. "
                + "Edit this file to change the blacklist — it is not editable in-game. "
                + "Default: 7 (Bedrock), 52 (Mob Spawner), 54 (Chest), "
                + "61 (Furnace), 62 (Burning Furnace), 63 (Sign Post), "
                + "64 (Wooden Door), 68 (Wall Sign), 71 (Iron Door).";
        blacklistSet = parseBlacklist(blacklistProp.value);

        forge.save();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isExcavationEnabled() { return excavationEnabled; }
    public int     getActivationKeyCode() { return activationKeyCode; }
    public int     getMaxBlocks()         { return maxBlocks; }
    public boolean isDamagePerBlock()     { return damagePerBlock; }
    public int     getOpenConfigKey()     { return openConfigKey; }
    public boolean isDebugMessages()      { return debugMessages; }

    /**
     * Returns the set of block IDs that are excluded from automatic excavation.
     * The set is unmodifiable; it is rebuilt from the config file on load.
     */
    public Set<Integer> getBlacklist()    { return blacklistSet; }

    // ── Setters — update in-memory value AND Property.value so save() persists ─
    // forge.Property.value is a public String field (confirmed by javap).

    public void setExcavationEnabled(boolean v) {
        excavationEnabled = v;
        enableProp.value  = Boolean.toString(v);
    }

    public void setActivationKeyCode(int v) {
        activationKeyCode = v;
        keyProp.value     = Integer.toString(v);
    }

    public void setMaxBlocks(int v) {
        maxBlocks       = v;
        maxProp.value   = Integer.toString(v);
    }

    public void setDamagePerBlock(boolean v) {
        damagePerBlock = v;
        durProp.value  = Boolean.toString(v);
    }

    public void setOpenConfigKey(int v) {
        openConfigKey      = v;
        openKeyProp.value  = Integer.toString(v);
    }

    public void setDebugMessages(boolean v) {
        debugMessages    = v;
        debugProp.value  = Boolean.toString(v);
    }

    // ── Persist to disk ───────────────────────────────────────────────────────

    /** Writes current property values to rorys-excavation.cfg. */
    public void save() {
        forge.save();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parses a comma-separated string of block IDs into a Set<Integer>.
     * Non-numeric tokens and IDs <= 0 are silently skipped.
     */
    private static Set<Integer> parseBlacklist(String csv) {
        Set<Integer> set = new HashSet<Integer>();
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.unmodifiableSet(set);
        }
        for (String token : csv.split(",")) {
            try {
                int id = Integer.parseInt(token.trim());
                if (id > 0) {
                    set.add(id);
                }
            } catch (NumberFormatException e) {
                // skip invalid token
            }
        }
        return Collections.unmodifiableSet(set);
    }
}
