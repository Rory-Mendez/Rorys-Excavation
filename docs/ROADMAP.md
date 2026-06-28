# Roadmap

This roadmap describes planned directions for Rory's Excavation. Nothing here is guaranteed or scheduled. Features are added when they are useful, stable, and fit the mod's philosophy: small, fast, compatible.

---

## Released

### v1.0.0
- **Stable release**. No new gameplay features.
- Dead code removed: `ExcavationDetector.onBlockBroken()`, `LOG`, `PREFIX`.
- Stale comments cleaned in `ExcavationHandler`, `GuiExcavationConfig`.
- `ARCHITECTURE.md` fully rewritten to match the actual codebase.
- `INSTALL.md` fully rewritten: all 7 config properties documented, jar name corrected, blacklist table added, key code reference tables added.

### v0.9.0
- **No debug spam in normal play**: excavation chat messages are suppressed by default.
- **`debugMessages` config option** (default: `false`): restore debug chat output when needed. Toggleable in the settings screen without restart.
- **Block blacklist** (`blacklist` in config): comma-separated block ID list. Blacklisted blocks are never excavated automatically; manual breaking is unaffected. File-only; not editable in-game.
- **Default blacklist**: Bedrock (7), Mob Spawner (52), Chest (54), Furnace (61), Burning Furnace (62), Sign Post (63), Wooden Door (64), Wall Sign (68), Iron Door (71).

### v0.8.0
- In-game settings screen (`GuiExcavationConfig`): press F12 (default, rebindable) to open.
- All five config values editable without touching the .cfg file: Enable Excavation, Activation Key, Max Blocks, Damage Mode, Open Config Key.
- Key-capture rebinding for Activation Key and Open Config Key: click button → press key → bound.
- Changes save to rorys-excavation.cfg on screen close and apply immediately (no restart).
- New config property: `openConfigKey` (default 88 = F12).

### v0.7.0
- Tool durability: `damagePerBlock=false` (default) deducts once per chain; `damagePerBlock=true` deducts once per extra block. Creative mode skipped. Empty hand safe. Native `damageItem` used — Unbreaking and modded tools work correctly.
- 26-neighbor BFS: faces, edges, and corners are all searched. Diagonal ore veins are now found and fully excavated.
- Confirmed mappings: `acq` = EntityLivingBase, `yw.av()` = getCurrentEquippedItem (hotbar slot), `yw.aT`/`qu.c` = isCreativeMode, `aan.a(int,acq)` = damageItem, `aan.b()` = getMaxDamage (virtual).

### v0.6.0
- Block drops for excavated blocks: extra blocks call `Block.dropBlockAsItemWithChance` before removal, preserving native and modded drop tables.
- Tightly clustered drops: newly spawned `EntityItem` objects are repositioned to the center of the original broken block and their velocity is zeroed, eliminating the vanilla random offset and upward kick that caused scatter.
- Extra blocks removed silently: no break sound or particles per extra block.
- Confirmed and documented: `pb` = Block, `pb.m` = blocksList[4096], `pb.a(xd,IIII,F,I)` = dropBlockAsItemWithChance, `xd.F` = isRemote (always false in SSP), `xd.b` = loadedEntityList, `nn.d(DDD)` = setPosition, `nn.r/s/t` = motionX/Y/Z.

### v0.5.0
- Full vein excavation: when activation key is held, all connected matching blocks (up to `maxBlocks`) are broken in one action.
- `ExcavationDetector.bfsCollectBlocks` — returns full BFS position list; `bfsFirstConnectedBlock` removed.
- Key not held: count-only reporting, no world changes.

### v0.4.0
- Activation-key gate: excavation only fires while the configured `activationKey` is held.
- Breaks exactly one extra connected matching block (first BFS result) when the key is held.
- `ExcavationDetector.WorldWriter` isolates world-modification calls from BFS logic.
- `xd.g(int,int,int,int)` confirmed as `setBlockWithNotify`; documented in Core obfuscation map.

### v0.3.0
- BFS connected-block search: after each detected break, runs a BFS from the 6 neighbors of the broken block, matching by block ID and metadata, capped at `maxBlocks` (default 64).
- `ExcavationDetector.WorldReader` interface isolates BFS logic from obfuscated MC types.
- Chat message: `[RorysExcavation] Found <count> connected blocks for id=<id> meta=<meta>`.
- Debug-only: no blocks are broken, no tool damage, no drops.

### v0.2.0
- Neighbor scan: after each detected break, inspects the 6 face-adjacent block positions and counts how many share the same block ID and metadata.
- Chat message: `[RorysExcavation] Found <n> matching neighbors for id=<id> meta=<meta>`.
- `ExcavationDetector.countMatchingNeighbors` — pure feature logic, no Forge coupling.

### v0.1.0
- Block-break detection: when the block at the player's crosshair disappears, the block ID, metadata, and world coordinates are logged to the Minecraft console.
- Confirmed and documented runtime obfuscated API mappings for `World.getBlockId`, `World.getBlockMetadata`, and `MovingObjectPosition`.
- Detection is gated by the `enableExcavation` config flag.

### v0.0.1
- Project scaffold: source layout, documentation, build workflow.
- Mod loads and appears in the Forge/FML mod list.
- Config file created on first launch.

---

## Post v1.0.0 Ideas

None of the items below are planned, scheduled, or promised. They are possible directions that may or may not be explored in future versions, evaluated on whether they fit the project's design philosophy and the constraints of Minecraft 1.2.5 / Forge 3.4.9.171.

### Configuration

- **Controls menu integration**: investigate whether the settings screen can be opened from Minecraft's built-in Controls menu, so players can discover and reach it without knowing the hotkey.
- **Options menu integration**: evaluate whether a shortcut or entry in the vanilla Options menu is worthwhile, or whether the F12 hotkey is sufficient.
- Regardless of how the screen is opened, the configurable hotkey (`openConfigKey`) should remain fully functional.

### Configuration GUI

- **In-game blacklist editing**: the blacklist is currently file-only. A future version may allow viewing and editing it directly from the settings screen.
- **Whitelist support**: consider whether an opt-in whitelist (only excavate listed block IDs) would complement the blacklist, or whether it would be redundant.
- Any block list changes must remain compatible with the existing `.cfg` file format — manual editing should always work.
- If block list editing is added to the GUI, consider a search or filter for block IDs to keep the interface usable as the list grows.

### Quality of Life

- **Configuration profiles**: named presets (e.g. Mining, Building, Custom) that let the player switch between different `maxBlocks`, activation key, and damage settings without manually editing values each time.
- **Import / Export presets**: share or back up configuration as a simple text snippet or file.
- **In-game feedback**: a subtle particle or sound cue when excavation triggers, so the player can tell it fired without relying on debug messages.
- **Per-tool opt-in**: optionally restrict excavation to pickaxes, shovels, or axes only; bare-handed or off-tool breaking would behave normally.

### Compatibility

- Continue testing with larger Minecraft 1.2.5 modpacks and common mods (OptiFine, Too Many Items, etc.).
- Investigate and document any conflicts that arise; improve compatibility where it is feasible within the 1.2.5 / Forge 3.4.9.171 constraints.
- Graceful degradation when conflicting mods are detected is preferred over hard failures.

### Rory's Mods Ecosystem Integration

- Future integration with other mods in the Rory's Mods ecosystem where it makes sense.
- Shared configuration conventions and obfuscation map maintenance via Rorys-Mod-Core.

---

## Design Philosophy

Rory's Excavation is intentionally narrow in scope. Future work should be evaluated against these principles before being added:

- **Lightweight**: no background threads, no large data structures, minimal per-tick work.
- **Predictable**: the player should always be able to anticipate what will and will not be excavated.
- **Configurable**: sensible defaults for new players, enough options for experienced ones.
- **Close to vanilla**: drops, durability, and sounds follow vanilla behaviour wherever possible.
- **Focused**: connected-block excavation is the one thing this mod does. Features that belong in a different mod should live in a different mod.

Feature creep is the primary risk to this project's long-term usability. When in doubt, the answer is no.

---

## Out of Scope (Forever)

- Server-side gameplay changes.
- Any feature that requires bytecode patching of `minecraft.jar`.
- Anything that gives a competitive advantage in PvP or that could be considered a cheat.
- Auto-mining without player block-break input.

---

*This roadmap is updated as priorities change. Check the [CHANGELOG](../CHANGELOG.md) for what has actually shipped.*
