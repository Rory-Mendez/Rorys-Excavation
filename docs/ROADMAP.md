# Roadmap

This roadmap describes planned directions for Rory's Excavation. Nothing here is guaranteed or scheduled. Features are added when they are useful, stable, and fit the mod's philosophy: small, fast, compatible.

---

## Released

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

## Planned

### vNext — Polish
- Block blacklist: config-driven list of block IDs to never excavate.

### Polish
- In-game feedback: particle or sound cue when excavation triggers.
- Better blacklist: config-driven list of block IDs to never excavate.
- Per-tool opt-in: only excavate when holding a pickaxe / shovel / axe (configurable).

### Configuration Improvements
- In-game config GUI (Forge mod options screen).
- Per-world config profiles.

### Compatibility
- Test and document compatibility with common 1.2.5 mods (OptiFine, Too Many Items, etc.).
- Graceful degradation when conflicting mods are detected.

### Rory's Ecosystem Integration
Future integration with other mods in the Rory's Mods ecosystem.

---

## Out of Scope (Forever)

- Server-side gameplay changes.
- Any feature that requires bytecode patching of `minecraft.jar`.
- Anything that gives a competitive advantage in PvP or that could be considered a cheat.
- Auto-mining without player block-break input.

---

*This roadmap is updated as priorities change. Check the [CHANGELOG](../CHANGELOG.md) for what has actually shipped.*
