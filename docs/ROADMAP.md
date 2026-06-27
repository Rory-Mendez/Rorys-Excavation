# Roadmap

This roadmap describes planned directions for Rory's Excavation. Nothing here is guaranteed or scheduled. Features are added when they are useful, stable, and fit the mod's philosophy: small, fast, compatible.

---

## Released

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

### vNext — Core Excavation
- Activation key detection (hold key + break block).
- BFS traversal of connected same-type blocks.
- Configurable `maxBlocks` limit (default 64).
- Configurable `damagePerBlock`: `false` = deduct durability once for the chain; `true` = once per block broken.
- Drops handled as close to vanilla behavior as possible.
- Block blacklist support.

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
