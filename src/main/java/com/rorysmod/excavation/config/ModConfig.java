package com.rorysmod.excavation.config;

import forge.Configuration;
import forge.Property;
import org.lwjgl.input.Keyboard;

import java.io.File;

/**
 * Loads and exposes all mod configuration values.
 *
 * Uses forge.Configuration (package: forge, NOT net.minecraftforge.common).
 * Confirmed present in Forge 3.4.9.171 client jar.
 *
 * API: getOrCreateBooleanProperty(String name, String category, boolean default)
 *      getOrCreateIntProperty(String name, String category, int default)
 *      getOrCreateStringProperty(String name, String category, String default)
 * All return forge.Property with .getBoolean(boolean) / .getInt(int) / .getString(String) methods.
 */
public class ModConfig {

    /** LWJGL key code for the grave / tilde key. */
    public static final int DEFAULT_ACTIVATION_KEY = Keyboard.KEY_GRAVE;

    public static final int DEFAULT_MAX_BLOCKS = 64;

    private final Configuration forge;

    private boolean excavationEnabled;
    private int     activationKeyCode;
    private int     maxBlocks;
    private boolean damagePerBlock;

    public ModConfig(File file) {
        this.forge = new Configuration(file);
    }

    public void load() {
        forge.load();

        Property enableProp = forge.getOrCreateBooleanProperty(
                "enableExcavation",
                Configuration.CATEGORY_GENERAL,
                true
        );
        enableProp.comment  = "Set to false to disable all excavation behavior.";
        excavationEnabled   = enableProp.getBoolean(true);

        Property keyProp = forge.getOrCreateIntProperty(
                "activationKey",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_ACTIVATION_KEY
        );
        keyProp.comment   = "LWJGL key code for the excavation activation key. "
                + "Default: " + DEFAULT_ACTIVATION_KEY + " (Grave / tilde `). "
                + "Hold this key while breaking a block to trigger excavation.";
        activationKeyCode = keyProp.getInt(DEFAULT_ACTIVATION_KEY);

        Property maxProp = forge.getOrCreateIntProperty(
                "maxBlocks",
                Configuration.CATEGORY_GENERAL,
                DEFAULT_MAX_BLOCKS
        );
        maxProp.comment = "Maximum number of connected blocks to break in one excavation action. "
                + "Range: 1–512. Default: " + DEFAULT_MAX_BLOCKS + ".";
        maxBlocks       = maxProp.getInt(DEFAULT_MAX_BLOCKS);

        Property durProp = forge.getOrCreateBooleanProperty(
                "damagePerBlock",
                Configuration.CATEGORY_GENERAL,
                false
        );
        durProp.comment = "If false (default), tool durability is deducted only once per excavation chain. "
                + "If true, durability is deducted once for every block broken.";
        damagePerBlock  = durProp.getBoolean(false);

        forge.save();
    }

    public boolean isExcavationEnabled() {
        return excavationEnabled;
    }

    public int getActivationKeyCode() {
        return activationKeyCode;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public boolean isDamagePerBlock() {
        return damagePerBlock;
    }
}
