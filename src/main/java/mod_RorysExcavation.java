// No package declaration — must be in the default (unnamed) package so ModLoader
// discovers it. ModLoader scans for classes named mod_* in the default package.

import com.rorysmod.excavation.config.ModConfig;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

import java.io.File;

/**
 * ModLoader/FML entry point for Rory's Excavation.
 *
 * Naming convention: mod_* classes in the default package are discovered by
 * ModLoader and wrapped by FMLModLoaderContainer. This class appears in the
 * Forge Mod List in-game.
 *
 * All real logic is delegated to com.rorysmod.excavation.* classes.
 */
public class mod_RorysExcavation extends BaseMod {

    public static final String MOD_NAME = "Rory's Excavation";
    public static final String VERSION  = "0.6.0";

    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * Called by ModLoader after all mod classes are instantiated.
     * Loads config and registers the excavation tick handler with FML.
     */
    @Override
    public void load() {
        File configDir = Loader.instance().getConfigDir();
        ModConfig config = new ModConfig(new File(configDir, "rorys-excavation.cfg"));
        config.load();

        FMLCommonHandler.instance().registerTickHandler(new ExcavationHandler(config));
    }
}
