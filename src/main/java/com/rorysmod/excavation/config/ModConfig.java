package com.rorysmod.excavation.config;

import forge.Configuration;
import forge.Property;
import org.lwjgl.input.Keyboard;

import java.io.File;

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
 *   public String value;   ← writable; updated by setters before save()
 *   public String comment;
 * forge.Configuration methods:
 *   getOrCreateBooleanProperty(String name, String category, boolean default) → Property
 *   getOrCreateIntProperty(String name, String category, int default) → Property
 *   save() → writes current property values to disk
 */
public class ModConfig {

    /** LWJGL key code for the grave / tilde key (default excavation activation). */
    public static final int DEFAULT_ACTIVATION_KEY  = Keyboard.KEY_GRAVE;
    public static final int DEFAULT_MAX_BLOCKS      = 64;
    /** LWJGL key code for F12 (default open-config key). */
    public static final int DEFAULT_OPEN_CONFIG_KEY = Keyboard.KEY_F12;

    private final Configuration forge;

    // ── Cached in-memory values ───────────────────────────────────────────────
    private boolean excavationEnabled;
    private int     activationKeyCode;
    private int     maxBlocks;
    private boolean damagePerBlock;
    private int     openConfigKey;

    // ── Property refs — needed to update values before save() ────────────────
    private Property enableProp;
    private Property keyProp;
    private Property maxProp;
    private Property durProp;
    private Property openKeyProp;

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

        forge.save();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isExcavationEnabled() { return excavationEnabled; }
    public int     getActivationKeyCode() { return activationKeyCode; }
    public int     getMaxBlocks()         { return maxBlocks; }
    public boolean isDamagePerBlock()     { return damagePerBlock; }
    public int     getOpenConfigKey()     { return openConfigKey; }

    // ── Setters — update in-memory value AND Property.value so save() persists ─
    // forge.Property.value is a public String field (confirmed by javap).
    // Setting it here and calling save() is the correct way to write new values
    // without re-constructing the Configuration object.

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

    // ── Persist to disk ───────────────────────────────────────────────────────

    /** Writes current property values to rorys-excavation.cfg. */
    public void save() {
        forge.save();
    }
}
