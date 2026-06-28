# Changelog

All notable changes to Rory's Excavation are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning: [Semantic Versioning](https://semver.org/).

---

## [1.0.0] — 2026-06-28

### Summary — v1.0.0

First stable release. All core features are complete and tested. No new gameplay features were added in this release — this is a cleanup, documentation, and polish pass to bring the codebase to release quality.

### Changed — v1.0.0

- **Version bump**: `mod_RorysExcavation.VERSION` set to `"1.0.0"`. Jar renamed `rorys-excavation-1.0.0.jar`.
- **Dead code removed**: `ExcavationDetector.onBlockBroken()`, `LOG`, and `PREFIX` fields deleted. The method was no longer called after v0.9.0; the two constants were its only dependents. The now-orphaned `java.util.logging.Logger` import was also removed.
- **Stale comments cleaned**:
  - `ExcavationHandler` class Javadoc updated to accurately describe the v0.9.0 tick logic (blacklist gate, debug-gated paths, count-only BFS only when `debugMessages=true`).
  - Removed the stale `yw.ah()` inline comment from the durability setup block — that method is never called.
  - Removed version-relative layout comment in `GuiExcavationConfig.rebuildButtons()`.
- **`ARCHITECTURE.md` fully rewritten**: was referencing `ExcavationLogic` (class that does not exist), `com.rorysmod.excavation.handler` (package that does not exist), and a stale two-class diagram. Now accurately documents all five classes, their responsibilities, and the design decisions behind them.
- **`INSTALL.md` fully rewritten**: was referencing `0.0.1.jar` and documented only 4 config properties. Now documents all 7 properties, the in-game settings screen, the default blacklist with rationale, key code reference tables, and updated jar name.
- **`BUILD.md`**: jar name updated to `1.0.0`.
- **`README.md`**: version updated to `1.0.0`.
- **`ROADMAP.md`**: v1.0.0 added to released section.

### No behavior changes — v1.0.0

All excavation behavior, config handling, blacklist enforcement, debug-message gating, GUI controls, and drop clustering are identical to v0.9.0.

---

## [0.9.0] — 2026-06-28

### Added — v0.9.0

- **`debugMessages` config option** (default: `false`): when `true`, excavation events are printed to in-game chat — block-break detection, connected-block count, and excavation total. When `false` (the default for all new installs), no excavation text appears in chat during normal gameplay. Toggleable in-game from the settings screen; takes effect immediately without restart.
- **Block blacklist** (`blacklist` config property): comma-separated list of block IDs that are never excavated automatically. Manual breaking always works normally. Invalid or out-of-range entries are silently ignored. Not editable in-game; change it in `rorys-excavation.cfg`.
- **Default blacklist** — `7,52,54,61,62,63,64,68,71`:

  | ID | Block | Reason |
  | --- | --- | --- |
  | 7 | Bedrock | Indestructible; excavating it does nothing but wastes cycles |
  | 52 | Mob Spawner | Removing spawners silently is almost always unintentional |
  | 54 | Chest | Auto-destroying a Chest would lose its inventory contents |
  | 61 | Furnace | Same inventory risk as Chest |
  | 62 | Burning Furnace | Same risk; active state implies contents are present |
  | 63 | Sign Post | Signs carry text data; silent removal is surprising |
  | 64 | Wooden Door | Doors are structural half-block entities |
  | 68 | Wall Sign | Same as Sign Post |
  | 71 | Iron Door | Same as Wooden Door |

- **Debug Messages toggle in GUI** (`GuiExcavationConfig`): settings screen now shows a "Debug Messages: true/false" toggle between Damage Mode and Open Config Key. All other GUI controls are unchanged.

### Changed — v0.9.0

- **Removed gameplay debug spam**: the three chat messages that appeared during normal play ("Block broken…", "Found X connected blocks…", "Excavated X extra blocks…") no longer appear unless `debugMessages=true`. Existing installations will get `debugMessages=false` added to their config file on the next launch.
- Count-only BFS (activation key not held) now only runs when `debugMessages=true`. When debug messages are off there is nothing to do on the count-only path, so the BFS is skipped entirely.
- `ExcavationDetector.onBlockBroken` console log no longer called from the tick handler (it was redundant; the data is captured by the debug chat path when `debugMessages=true`).

### Confirmed new API — v0.9.0

- `forge.Configuration.getOrCreateProperty(String, String, String)` — the string-value variant of the Forge 3.4.9.171 config API. (`getOrCreateStringProperty` does NOT exist in this Forge version.)

---

## [0.8.0] — 2026-06-28

### Added - v0.8.0

- **In-game settings screen** (`GuiExcavationConfig`): press **F12** (default) while in-game with no GUI open to open the Rory's Excavation settings screen. All five config values are editable without touching the config file:
  - **Enable Excavation** — toggle button.
  - **Activation Key** — click to enter key-capture mode; press any key to bind; ESC cancels.
  - **Max Blocks** — `<` and `>` buttons adjust in steps of 8, clamped 1–512.
  - **Damage Mode** — toggles between *Per Chain* (one durability hit per excavation) and *Per Block* (one per extra block removed).
  - **Open Config Key** — the F12 key itself is rebindable in the same screen using the same key-capture mechanism.
- **`openConfigKey` config property** (default: 88 = F12): written to `rorys-excavation.cfg` on first launch. Changing it in the file or in-game both work. F12 was chosen over F7 to avoid conflicts with MAtmos and other legacy 1.2.5 mods.
- **Immediate apply**: new values take effect as soon as the settings screen closes — no game restart required. `ExcavationHandler` reads `config.getActivationKeyCode()`, `config.getOpenConfigKey()`, etc. from the live `ModConfig` object every tick.
- **Config file remains fully compatible**: the .cfg format is unchanged; manual editing still works.

### How the GUI works - v0.8.0

`GuiExcavationConfig extends vp` (GuiScreen). `ExcavationHandler.tickStart` detects the leading edge of the `openConfigKey` press (edge-detected via `prevOpenKeyDown`, fires once per press not once per tick) and calls `mc.a(new GuiExcavationConfig(config))` = `displayGuiScreen`. On close, `vp.j()` (onGuiClosed) calls `config.setXxx()` for each edited value then `config.save()`. `forge.Property.value` (a public String field) is updated by each setter; `forge.Configuration.save()` flushes the property map to disk.

Key-capture mode: clicking an Activation Key or Open Config Key button sets `capturingForBtn`. The screen's `vp.a(CI)V` = `keyTyped` override intercepts the next non-ESC key press and records it as the new binding. ESC cancels without changing anything.

### Confirmed new obfuscated mappings — v0.8.0

See Core `docs/OBFUSCATION_MAP.md` for full entries. Summary:

| Obfuscated | Deobfuscated | Notes |
| --- | --- | --- |
| `vp` | `GuiScreen` | Extends `oo`. Fields: `p`=mc, `q`=width, `r`=height, `s`=buttonList (List, protected), `u`=fontRenderer (nl, protected). |
| `abp` | `GuiButton` | Extends `oo`. Fields: `a`=id (int, protected), `e`=displayString (String), `h`=enabled (boolean). Constructor `(IIIIILjava/lang/String;)` = (id,x,y,width,height,text). |
| `nl` | `FontRenderer` | `a(String,int,int,int)I` = drawString; `a(String)I` = getStringWidth. |
| `mc.a(vp)V` | `displayGuiScreen(GuiScreen)` | Opens a screen; triggers `onGuiClosed()` on the old screen. |
| `forge.Property.value` | Writable String field | Set directly to update a property value before `forge.Configuration.save()`. |

### Not yet implemented - v0.8.0

- Block blacklist / whitelist.
- Controls menu integration (opening from Minecraft's Options/Controls screens).
- Per-tool opt-in.

---

## [0.7.0] — 2026-06-28

### Added - v0.7.0

- **Tool durability** (`damagePerBlock` config option):
  - `damagePerBlock=false` (default): the held tool loses exactly **1 durability** for the entire excavation chain, matching the single block the player manually broke.
  - `damagePerBlock=true`: the held tool loses **1 durability per extra block** removed by excavation.
- **Creative mode safe**: no durability is consumed when the player's capabilities flag `qu.c` (isCreativeMode) is true.
- **Empty hand safe**: null check on `yw.av()` (getCurrentEquippedItem) prevents any crash when excavating bare-handed.
- **Modded tool compatible**: uses `ItemStack.damageItem` (`aan.a(int, acq)`) — the same native method vanilla calls when breaking blocks — so Unbreaking enchantment, tool-break animation, inventory cleanup, and modded tool overrides all work correctly.
- **Tool break handled correctly**: when a tool's durability runs out mid-chain, `damageItem` plays the vanilla break animation and sound once, sets stackSize to 0, and subsequent iterations are guarded (`tool.a > 0`) to prevent redundant break effects.
- **26-neighbor BFS** (replaces 6-face): `ExcavationDetector` now checks all 26 neighboring positions per block — faces (6), edges (12), and corners (8). Diagonal ore veins and blocks touching only at edges or corners are now found and excavated correctly.
- Confirmed and documented new obfuscated mappings: `acq` = EntityLivingBase, `yw.av()` = getCurrentEquippedItem (via `aak.b()` = `ap.a[ap.c]`), `yw.ah()` = getItemInUse (null unless eating/drinking), `yw.aT` = PlayerCapabilities (`qu`), `qu.c` = isCreativeMode, `aan.a(int,acq)` = damageItem, `aan.b()` = getMaxDamage via virtual `yr.g(aan)`, `aan.a` = stackSize.

### How durability works - v0.7.0

`mc.h.av()` returns the `aan` (ItemStack) in the selected hotbar slot, or null for empty hand. If null, or if `mc.h.aT.c` (isCreativeMode) is true, or if `tool.b()` (virtual getMaxDamage) returns 0, no damage is applied. Otherwise `tool.a(1, mc.h)` = `damageItem(1, player)` is called: it checks the Unbreaking enchantment level via `ais.c(yw.ap)` and uses `world.random.nextInt(level+1) > 0` to randomly skip damage. If damage is applied and `itemDamage > maxDamage`, vanilla destroys the item (`acq.c(aan)` plays the break animation, stackSize is decremented to 0).

### How 26-neighbor BFS works - v0.7.0

`ExcavationDetector.NEIGHBORS` is a static `int[26][3]` generated once over all `dx, dy, dz ∈ {-1, 0, 1}` except `(0,0,0)`. `seedQueue` iterates all 26 offsets per visited block. All other BFS logic (visited `HashSet<Long>`, `posKey` encoding, `maxBlocks` cap) is unchanged from v0.5.0.

### Not yet implemented - v0.7.0

- Blacklist.

---

## [0.6.0] — 2026-06-27

### Added - v0.6.0

- **Block drops for excavated blocks**: extra blocks now drop their items. The game's own `Block.dropBlockAsItemWithChance` (`pb.a(xd,int,int,int,int,float,int)` — confirmed by `javap`) is called for every extra block before it is removed. Drop chance is 1.0 (100%) and fortune is 0.
- **Tightly clustered drops**: all item drops are repositioned to the center of the original broken block (`prevX+0.5, prevY+0.5, prevZ+0.5`) with zero velocity after spawning, so every item lands in one spot instead of scattering.
- **Silent harvesting**: extra blocks are removed without triggering block-break sound or particles. Only the vanilla feedback from the player's own break is heard.
- Confirmed and documented new obfuscated mappings from `javap` inspection: `pb` = Block class, `pb.m` = Block.blocksList (static `pb[]`, size 4096), `pb.a(xd,int,int,int,int,float,int)` = `dropBlockAsItemWithChance`, `xd.F` = `isRemote` (always `false` for `mc.f` in SSP), `xd.b` = loadedEntityList, `nn.d(DDD)` = setPosition, `nn.r/s/t` = motionX/Y/Z. See Core `docs/OBFUSCATION_MAP.md` and `docs/BUILD.md`.

### How drops work - v0.6.0

`pb.m[blockId]` is the Block instance for the excavated block type. `dropBlockAsItemWithChance` calls `idDropped` and `quantityDropped` on the block (preserving modded drop tables), then spawns an `EntityItem` (`fq`) via `xd.a(nn)` = `spawnEntityInWorld`, which adds it to `xd.b` (loadedEntityList). The vanilla spawner applies a random `[+0.15, +0.85]` position offset and sets `motionY = 0.2` (upward kick) plus random X/Z velocity — the cause of scatter. After each drop call, this mod snapshots how many entities were in `xd.b` before the call, then iterates new entries: each `fq` is repositioned via `nn.d(cx, cy, cz)` = setPosition and its `r/s/t` (motionX/Y/Z) fields are zeroed.

### Not yet implemented - v0.6.0

- Tool damage, blacklist.

---

## [0.5.0] — 2026-06-27

### Added - v0.5.0

- **Full vein excavation**: when the activation key is held, BFS now collects all connected matching blocks (up to `maxBlocks`, default 64) and removes every one of them via `World.setBlockWithNotify`. Previously only one extra block was broken.
- `ExcavationDetector.bfsCollectBlocks` — BFS variant that returns `List<int[]>` of all matching connected positions (capped at `maxBlocks`). Used by the excavation path so the caller can iterate and break each block.
- `ExcavationHandler` now branches on key state: key held → `bfsCollectBlocks` + remove loop; key not held → `bfsConnectedBlocks` count only (no allocation, no world change).
- Chat message when key held: `[RorysExcavation] Excavated <count> extra blocks for id=<id> meta=<meta>`.

### Removed - v0.5.0

- `ExcavationDetector.bfsFirstConnectedBlock` — superseded by `bfsCollectBlocks`. The v0.4.0 single-block path is replaced by the full-vein loop.

### Not yet implemented - v0.5.0

- Tool damage, drops, blacklist.

---

## [0.4.0] — 2026-06-27

### Added - v0.4.0

- **Activation-key gate**: excavation behavior (extra block breaking) only fires when the configured `activationKey` (default: Grave/tilde, LWJGL key 41) is held at the moment the player's block break is detected. Without the key the mod reports detection and BFS count only — no world modifications.
- `ExcavationDetector.WorldWriter` — minimal single-method interface (`setBlock`) that lets block-removal logic call into the obfuscated world without touching `xd` types directly. Implemented in `ExcavationHandler` using confirmed `xd.g(int,int,int,int)`.
- `ExcavationDetector.bfsFirstConnectedBlock` — returns the coordinates of the first BFS-reachable connected block (or `null`), used to select the one extra block to break.
- `ExcavationDetector.removeBlock(WorldWriter, x, y, z)` — thin wrapper that calls `writer.setBlock(x, y, z, 0)`, isolating world modification from BFS logic.
- `ExcavationDetector.seedQueue` extracted as a private helper to eliminate duplication between `bfsConnectedBlocks` and `bfsFirstConnectedBlock`.
- `ExcavationHandler` now calls `xd.g(x,y,z,0)` (confirmed as `World.setBlockWithNotify`) to remove the chosen block, triggering proper chunk dirty-marking and neighbor block-update notifications.
- New chat messages when key is held: `[RorysExcavation] Excavated 1 extra block id=<id> meta=<meta> pos=(x,y,z)`.
- Confirmed obfuscated mapping `xd.g(int,int,int,int)` = `setBlockWithNotify` documented in Core `docs/OBFUSCATION_MAP.md` (verified by `javap -c` bytecode inspection).

### Not yet implemented - v0.4.0

- Breaking more than one extra block (full vein excavation planned for a future release).
- Tool damage, drops, blacklist.

---

## [0.3.0] — 2026-06-27

### Added - v0.3.0

- `ExcavationDetector.WorldReader` — minimal two-method interface (`getBlockId`, `getBlockMeta`) that lets BFS logic query the world without touching obfuscated Minecraft types.
- `ExcavationDetector.bfsConnectedBlocks` — BFS traversal (not recursive DFS) that starts from the 6 immediate neighbors of the broken block, expands face-adjacently, matches by block ID and metadata, and caps at `maxBlocks` (default 64). Uses `HashSet<Long>` for visited tracking via a packed-long position key.
- `ExcavationHandler` now creates an anonymous `WorldReader` that delegates to the confirmed `xd.a` / `xd.e` world methods and passes it to BFS after each detected break.
- New in-game chat message after each break: `[RorysExcavation] Found <count> connected blocks for id=<id> meta=<meta>`.

### Not yet implemented - v0.3.0

- No blocks are broken (debug-only release).
- No tool damage, no drops, no blacklist.

---

## [0.2.0] — 2026-06-27

### Added - v0.2.0

- `ExcavationDetector.countMatchingNeighbors` — pure logic method that counts how many of the 6 face-adjacent blocks share the same block ID and metadata as the broken block.
- `ExcavationHandler` now reads the 6 orthogonal neighbor positions (+X, -X, +Y, -Y, +Z, -Z) after each detected break using the confirmed `xd.a` / `xd.e` world methods.
- New in-game chat message after each break: `[RorysExcavation] Found <n> matching neighbors for id=<id> meta=<meta>`.

### Not yet implemented - v0.2.0

- BFS traversal or actual block breaking (planned for a future release).

---

## [0.1.0] — 2026-06-27

### Added - v0.1.0

- `ExcavationHandler` — GAME-tick handler that polls `Minecraft.objectMouseOver` (runtime: `mc.z`) to detect when a targeted block disappears.
- `ExcavationDetector` — pure feature class (no Forge/LWJGL coupling) that logs detected breaks to the Minecraft console: block ID, metadata, and world coordinates.
- `ExcavationHandler` also sends an in-game chat message on each detected break via `GuiIngame.printChatMessage` (runtime: `mc.w.a(String)`), making detection immediately visible without needing an external log viewer.
- Block-break detection is gated by the `enableExcavation` config flag.
- Confirmed and documented runtime obfuscated mappings for `World.getBlockId` (`xd.a`), `World.getBlockMetadata` (`xd.d`), `MovingObjectPosition` (`pl`), `GuiIngame` (`aiy`), and `GuiIngame.printChatMessage` (`aiy.a`).

### Not yet implemented - v0.1.0

- BFS traversal or multi-block breaking (planned for a future release).

---

## [0.0.1] — 2026-06-27

### Added - v0.0.1

- Project scaffold: source layout, build instructions, documentation.
- `mod_RorysExcavation` entry point — mod loads and appears in the Forge/FML mod list.
- Config file created on first launch at `.minecraft/config/rorys-excavation.cfg`.
- Config properties defined: `enableExcavation`, `activationKey`, `maxBlocks`, `durabilityMode`.

### Not yet implemented - v0.0.1

- Excavation behavior (planned for v0.1.0).
