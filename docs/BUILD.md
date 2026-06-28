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

### `xd` (World) fields

| MCP name | Runtime field | Type | Notes |
|---|---|---|---|
| `loadedEntityList` | `b` | `List` | All live non-player entities in the loaded world. `spawnEntityInWorld` adds non-player entities here. Snapshot `.size()` before a drop call, then iterate from that index after the call to find newly spawned entities. Confirmed from `spawnEntityInWorld` bytecode: `getfield b:List; aload entityArg; invokeinterface List.add`. |
| `isRemote` | `F` | `boolean` | See `pb` section below. |

### `xd` (World) methods

| MCP name | Runtime method | Confirmed by |
|---|---|---|
| `getBlockId(x, y, z)` | `a(int, int, int)` | `javap` confirms delegation to `ack.a:(III)I` |
| `getBlockMetadata(x, y, z)` | `e(int, int, int)` | `javap` confirms delegation to `ack.c:(III)I` |
| `getBlockLightValue(x, y, z)` | `d(int, int, int)` | Delegates to `ack.b:(III)I`; returns combined sky+block light 0–255. **Not metadata.** |
| `setBlockWithNotify(x, y, z, blockId)` | `g(int, int, int, int)` | Writes block via `d(IIII)Z` (→ `ack.a:(IIII)Z` + marks dirty), then notifies neighbors via `h(IIII)V` (→ `k(III)V` + `j(IIII)V`). Returns `boolean`. Pass `blockId=0` to remove a block. Confirmed by `javap -c` during v0.4.0 development. |
| `spawnEntityInWorld(entity)` | `a(nn)` | Non-player entity path: adds to `xd.b` (loadedEntityList) and calls `ack.a(nn)` (chunk entity list). Returns `boolean`. Confirmed by `javap -c`. |

### `vq` (EntityClientPlayerMP) / `yw` (EntityPlayer) methods

| MCP name | Runtime method |
|---|---|
| `isSneaking()` | `V()` |

### `pb` (Block) class — confirmed v0.6.0

`pb` is the Block base class. Confirmed by `javap` of `minecraft-1.2.5-client.jar`: it holds all static block instances and the global `blocksList` array.

| MCP name | Runtime identifier | Type / signature | Notes |
|---|---|---|---|
| `Block` | `pb` | class | Base class for all blocks. Vanilla and modded blocks extend `pb` directly or through subclasses. |
| `Block.blocksList` | `pb.m` | `static pb[]` (size 4096) | The global block registry, indexed by block ID. `pb.m[0]` = air (null). Size is 4096 — Forge-extended range. |
| `Block.dropBlockAsItemWithChance(world,x,y,z,meta,chance,fortune)` | `pb.a(xd, int, int, int, int, float, int)` | `void` | Calls `idDropped(meta,rand,fortune)` + `quantityDropped(fortune,rand)` on the block, then spawns an `EntityItem` (`fq`) via `xd.a(nn)`. Guards on `xd.F` (isRemote): returns early if `true`. In SSP `mc.f.F` is always `false`, so drops run. |
| `Block.dropBlockAsItem(world,x,y,z,meta,fortune)` | `pb.a(xd, int, int, int, int, int)` (final) | `void` | Convenience wrapper: calls `dropBlockAsItemWithChance` with `chance=1.0f`. |
| `Block.idDropped(meta, rand, fortune)` | `pb.a(int, Random, int)` | `int` | Returns the item ID that this block drops. Override in subclass for custom drops. |
| `Block.quantityDroppedWithBonus(fortune, rand)` | `pb.a(int, Random)` | `int` | Returns the drop count given fortune level. |

#### `xd.F` — `World.isRemote` (critical for drops)

`xd.F` is a `boolean` field on `xd` (World). `dropBlockAsItemWithChance` reads it and returns early (no drops) if `true`.

Confirmed by `javap -c` on `xd.class`: all three `xd` constructors set `F = false` (`iconst_0; putfield F:Z`). The only World subclass in the client jar (`je extends xd`) also never sets `F = true`. Therefore `mc.f.F == false` in SSP, and `dropBlockAsItemWithChance` executes fully when called on `mc.f`.

#### Supporting classes

| Obfuscated | Deobfuscated | Notes |
|---|---|---|
| `fq` | `EntityItem` | Extends `nn` (Entity). Has field `fq.a` (type `aan` = ItemStack) and `fq.c` (int pickup delay, set to 10 on spawn). |
| `aan` | `ItemStack` | Item + count + damage. Used as the `fq.a` payload. |
| `je` | *(WorldClient subclass)* | The only xd subclass in the client jar. Constructor takes `adl, fj, int, int`; passes `"MpServer"` to the xd base constructor. Never sets `xd.F = true`. |

### `acq` (EntityLivingBase), `yw` (EntityPlayer), `qu` (PlayerCapabilities) — durability

Confirmed by `javap` of `minecraft-1.2.5-client.jar` during Rory's Excavation v0.7.0 development.

| Obfuscated | MCP name | Type | Notes |
|---|---|---|---|
| `acq` | `EntityLivingBase` | class | Abstract living entity. Parent of `yw` (EntityPlayer). `aan.a(int, acq)` = damageItem takes this type. `vq extends yw extends acq`, so `mc.h` is a valid `acq`. |
| `yw.ah()` | `getCurrentEquippedItem()` | `aan` | Returns `yw.d` (private `aan` field = held ItemStack). Returns `null` when the player's hand is empty. Always null-check before using. |
| `yw.aT` | `PlayerCapabilities` | `qu` | Public field. Holds capability booleans for the player. |
| `qu.c` | `isCreativeMode` | `boolean` | Confirmed: `yw.e(float)` = addExhaustion returns immediately when `qu.c` is true. `damageItem` does NOT check creative mode internally in 1.2.5 — the caller must guard it. |
| `qu.a` | `disableDamage` | `boolean` | Confirmed: `yw.a(md, int)` = attackEntityFrom returns false (no damage) when `qu.a` is true. |

#### `aan` (ItemStack) — durability methods

| Signature | MCP name | Notes |
|---|---|---|
| `aan.e()` | `isItemStackDamageable()` | Returns true if `yr.h() > 0` (item has max durability > 0). Returns false for items that cannot be damaged (blocks in hand, food, etc.). `damageItem` also checks this internally and returns early if false, but checking first is cleaner. |
| `aan.a(int, acq)` | `damageItem(int amount, EntityLivingBase entity)` | The complete vanilla damage method. Guards: (1) `e()` = isItemStackDamageable, if false return. (2) If entity is `yw` (EntityPlayer): reads `yw.ap` = InventoryPlayer, calls `ais.c(aak)` = EnchantmentHelper.getUnbreakingModifier; if modifier > 0, calls `world.random.nextInt(modifier+1)` and returns (skip damage) if result > 0. (3) `e:I += amount` (itemDamage += amount). (4) If `e:I > j()` (maxDamage): calls `acq.c(aan)` = renderBrokenItemStack (plays break animation/sound), updates stat via `yw.a(ajw, 1)`, decrements `a:I` (stackSize), clamps to 0, resets `e:I` = 0. |
| `aan.a` | `stackSize` | `public int`. Guard `> 0` before calling damageItem to avoid triggering the break animation multiple times if the tool already broke on a previous iteration. |
| `aan.h()` / `aan.i()` | `getItemDamage()` | Both return field `e:I` (itemDamage). Two aliases for the same field. |
| `aan.b(int)` | `setItemDamage(int)` | Puts int into field `e:I`. Direct write — no Unbreaking, no break check. Use `damageItem` instead. |

### `nn` (Entity) fields — motion and position

| MCP name | Runtime field | Type | Notes |
|---|---|---|---|
| `posX` | `nn.o` | `double` | Confirmed: `nn.d(DDD)` puts arg1 into field `o`. |
| `posY` | `nn.p` | `double` | Confirmed: `nn.d(DDD)` puts arg2 into field `p`. |
| `posZ` | `nn.q` | `double` | Confirmed: `nn.d(DDD)` puts arg3 into field `q`. |
| `motionX` | `nn.r` | `double` | Confirmed: `fq` constructor puts `rand*0.2-0.1` into field `r`. Public on `nn`. |
| `motionY` | `nn.s` | `double` | Confirmed: `fq` constructor puts `0.2` into field `s`. Public on `nn`. |
| `motionZ` | `nn.t` | `double` | Confirmed: `fq` constructor puts `rand*0.2-0.1` into field `t`. Public on `nn`. |

### `nn` (Entity) methods — position

| Signature | MCP name | Notes |
|---|---|---|
| `nn.d(double, double, double)` | `setPosition(x, y, z)` | Sets `nn.o/p/q` (posX/Y/Z) and updates the bounding box `nn.y` (AxisAlignedBB). Use this rather than setting `o/p/q` directly so the AABB stays in sync. Confirmed from `nn.d(DDD)` bytecode. |

#### Vanilla spawn offset (why drops scatter without the v0.7.0 fix)

`Block.a(xd, int, int, int, aan)` (the protected internal spawner called by `dropBlockAsItemWithChance`) applies:
```
float s = 0.7f
spawnX = blockX + rand.nextFloat() * s + (1-s)*0.5   // → blockX + [+0.15, +0.85]
spawnY = blockY + rand.nextFloat() * s + (1-s)*0.5
spawnZ = blockZ + rand.nextFloat() * s + (1-s)*0.5
```
The `fq` constructor additionally sets `nn.r = rand*0.2 - 0.1`, `nn.s = 0.2` (upward), `nn.t = rand*0.2 - 0.1`. The fixed upward kick combined with random horizontal velocity is the primary cause of scatter across many ticks. The v0.7.0 fix calls `nn.d(cx, cy, cz)` and zeros `r/s/t` on every spawned `fq` immediately after `dropBlockAsItemWithChance` returns.

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
| LWJGL | 2.9.4 | Prism caches at `PrismLauncher/libraries/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar` |

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
  src\main\java\GuiExcavationConfig.java ^
  src\main\java\com\rorysmod\excavation\config\ModConfig.java ^
  src\main\java\com\rorysmod\excavation\feature\ExcavationDetector.java
```

### Step 2 — Package

```bat
cd build\classes
jar cf ..\..\rorys-excavation-1.0.0.jar .
```

### Step 3 — Verify

```bat
jar -tf rorys-excavation-1.0.0.jar
```

Expected contents:

```
META-INF/MANIFEST.MF
mod_RorysExcavation.class
ExcavationHandler.class
ExcavationHandler$1.class
ExcavationHandler$2.class
GuiExcavationConfig.class
com/rorysmod/excavation/config/ModConfig.class
com/rorysmod/excavation/feature/ExcavationDetector.class
com/rorysmod/excavation/feature/ExcavationDetector$WorldReader.class
com/rorysmod/excavation/feature/ExcavationDetector$WorldWriter.class
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
| Config API | `getOrCreateBooleanProperty(name, category, default)` / `getOrCreateIntProperty(name, category, default)` / `getOrCreateProperty(name, category, default)` (string variant — `getOrCreateStringProperty` does NOT exist in this version) |
| Key detection | `Keyboard.isKeyDown(keyCode)` from LWJGL — no FML key-bind registration API for this version |
| Minecraft instance | `FMLClientHandler.instance().getClient()` returns `net.minecraft.client.Minecraft` |
