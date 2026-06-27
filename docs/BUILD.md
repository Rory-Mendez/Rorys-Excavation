# Building Rory's Excavation

Target: Minecraft 1.2.5 · Forge 3.4.9.171 · FML 2.2.106.176

---

## What the build actually requires

This mod is compiled directly against the unmodified Minecraft 1.2.5 + Forge 3.4.9.171 jars — **no MCP workspace, no stubs required.**

All Minecraft game classes are obfuscated (e.g. `EntityPlayer` is `yw`, `Block` is `air`-mapped, `World` is `xd`).  
The mod source uses these obfuscated names directly, confirmed by decompiling the actual runtime jars with `javap`.

The JVM resolves field and method references by **name and type descriptor**. The compiled bytecode must use  
the exact obfuscated descriptors from the runtime jars — this is why stub-based compilation with `Object` types fails.

---

## Runtime obfuscated name mapping

All mappings confirmed by `javap` on `minecraft-1.2.5-client.jar` and `forge-1.2.5-3.4.9.171-client.jar`.

### `net.minecraft.client.Minecraft` fields

| MCP name | Runtime field | Runtime type |
|---|---|---|
| `thePlayer` | `h` | `vq` (EntityClientPlayerMP) |
| `theWorld` | `f` | `xd` (World) |
| `currentScreen` | `s` | `vp` (GuiScreen) |
| `gameSettings` | `A` | `hu` (GameSettings) |
| `objectMouseOver` | `z` | `pl` (MovingObjectPosition) |
| `ingameGUI` | `w` | `aiy` (GuiIngame) |

### `aiy` (GuiIngame) methods

| MCP name | Runtime method | Confirmed by |
|---|---|---|
| `printChatMessage(String)` | `a(String)` | bytecode: wraps string in `nt`, adds to chat `List` |

`aiy.b(String)` = `setRecordPlayingMessage` ("Now playing: …"). `aiy.c(String)` = translated chat (calls `a` internally). Use `a(String)` for plain messages.

### `pl` (MovingObjectPosition) fields

| MCP name | Runtime field | Type | Notes |
|---|---|---|---|
| `blockX` | `b` | `int` | Valid when `entityHit == null` |
| `blockY` | `c` | `int` | Valid when `entityHit == null` |
| `blockZ` | `d` | `int` | Valid when `entityHit == null` |
| `entityHit` | `g` | `nn` | `null` when targeting a block; non-null when targeting an entity |

When `objectMouseOver` itself is `null`, the player is targeting nothing (MISS).  
When `pl.g == null`, the target is a block and `b/c/d` are valid coordinates.  
When `pl.g != null`, the target is an entity and `b/c/d` should not be used.

### `xd` (World) methods

| MCP name | Runtime method | Confirmed by |
|---|---|---|
| `getBlockId(x, y, z)` | `a(int, int, int)` | `javap` confirms delegation to `ack.a:(III)I` |
| `getBlockMetadata(x, y, z)` | `e(int, int, int)` | `javap` confirms delegation to `ack.c:(III)I` |
| `getBlockLightValue(x, y, z)` | `d(int, int, int)` | Delegates to `ack.b:(III)I`; returns combined sky+block light 0–255. **Not metadata.** |
| `setBlockWithNotify(x, y, z, blockId)` | `g(int, int, int, int)` | Writes block via `d(IIII)Z` (→ `ack.a:(IIII)Z` + marks dirty), then notifies neighbors via `h(IIII)V` (→ `k(III)V` + `j(IIII)V`). Returns `boolean`. Pass `blockId=0` to remove a block. Confirmed by `javap -c` during v0.4.0 development. |

### `vq` (EntityClientPlayerMP) / `yw` (EntityPlayer) methods

| MCP name | Runtime method |
|---|---|
| `isSneaking()` | `V()` |

---

## Where the compiled mod goes

**`mods/` folder — NOT patched into `minecraft.jar`.**

This mod uses a default-package `mod_RorysExcavation` class (discovered by ModLoader) which wires FML  
components together. Forge scans `mods/` for jars on startup.

---

## Why mod code is in the default package

Named-package Java code (`com.example.*`) cannot import or reference default-package classes.  
All obfuscated Minecraft game classes (`vq`, `hu`, `xd`, `yw`, etc.) live in the default package.  
Therefore, any code that touches Minecraft types must also be in the default package:

- `mod_RorysExcavation.java` — ModLoader entry point
- `ExcavationHandler.java` — tick handler (will access obfuscated MC types when excavation is implemented)

Named-package code (`ModConfig`) is fine — it only touches Forge config APIs, not Minecraft types.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java JDK | 8+ | Must be a JDK (includes `javac`). JDK 21 works with `-source 8 -target 8`; warnings about obsolete source/target are safe to ignore. JDK 8 is ideal to avoid them. |
| `minecraft-1.2.5-client.jar` | 1.2.5 | Prism caches at `PrismLauncher/libraries/com/mojang/minecraft/1.2.5/` |
| `forge-1.2.5-3.4.9.171-client.jar` | 3.4.9.171 | Prism caches at `PrismLauncher/libraries/net/minecraftforge/forge/1.2.5-3.4.9.171/` |
| LWJGL | 2.x | Prism caches at `PrismLauncher/libraries/org/lwjgl/` |

---

## Compile and package

### Step 1 — Compile

Put the MC jar **before** the Forge jar so the full set of obfuscated game classes resolves from the MC jar.

```bat
set JAVAC=path\to\jdk8\bin\javac.exe
set MC=path\to\minecraft-1.2.5-client.jar
set FORGE=path\to\forge-1.2.5-3.4.9.171-client.jar
set LWJGL=path\to\lwjgl.jar
set MOD_OUT=build\classes

mkdir build\classes
%JAVAC% -source 8 -target 8 ^
  -classpath "%MC%;%FORGE%;%LWJGL%" ^
  -sourcepath src\main\java ^
  -d %MOD_OUT% ^
  src\main\java\mod_RorysExcavation.java ^
  src\main\java\ExcavationHandler.java ^
  src\main\java\com\rorysmod\excavation\config\ModConfig.java ^
  src\main\java\com\rorysmod\excavation\feature\ExcavationDetector.java
```

### Step 2 — Package

```bat
cd build\classes
jar cf ..\..\rorys-excavation-0.4.0.jar .
```

### Step 3 — Verify

```bat
jar -tf rorys-excavation-0.4.0.jar
```

Expected contents:

```
META-INF/MANIFEST.MF
mod_RorysExcavation.class
ExcavationHandler.class
com/rorysmod/excavation/config/ModConfig.class
com/rorysmod/excavation/feature/ExcavationDetector.class
```

### Step 4 — Install

Drop `rorys-excavation-0.0.1.jar` into the Prism Launcher instance `mods/` folder and launch.

---

## API notes (confirmed from the actual Forge 3.4.9.171 jar)

| API | Correct for this version |
|---|---|
| Entry point | `mod_RorysExcavation` in the default package extending `BaseMod` — ModLoader scans for `mod_*` classes at jar root |
| Lifecycle | `BaseMod.load()` called by ModLoader. `@Mod` lifecycle annotations exist but `FMLModContainer.preInit/init/postInit` are empty stubs — never invoked in FML 2.2.x |
| Tick registration | `FMLCommonHandler.instance().registerTickHandler(ITickHandler)` — no `Side` parameter |
| Tick types available | `GAME`, `RENDER`, `GUI`, `WORLDGUI`, `GUILOAD`, `WORLD` — `PLAYER` does NOT exist |
| Config class | `forge.Configuration` (NOT `net.minecraftforge.common.Configuration`) |
| Config API | `getOrCreateBooleanProperty(name, category, default)` / `getOrCreateIntProperty(name, category, default)` / `getOrCreateStringProperty(name, category, default)` |
| Key detection | `Keyboard.isKeyDown(keyCode)` from LWJGL — no FML key-bind registration API for this version |
| Minecraft instance | `FMLClientHandler.instance().getClient()` returns `net.minecraft.client.Minecraft` |
