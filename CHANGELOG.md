# Changelog

All notable changes to Rory's Excavation are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning: [Semantic Versioning](https://semver.org/).

---

## [0.5.0] — 2026-06-27

### Added
- **Full vein excavation**: when the activation key is held, BFS now collects all connected matching blocks (up to `maxBlocks`, default 64) and removes every one of them via `World.setBlockWithNotify`. Previously only one extra block was broken.
- `ExcavationDetector.bfsCollectBlocks` — BFS variant that returns `List<int[]>` of all matching connected positions (capped at `maxBlocks`). Used by the excavation path so the caller can iterate and break each block.
- `ExcavationHandler` now branches on key state: key held → `bfsCollectBlocks` + remove loop; key not held → `bfsConnectedBlocks` count only (no allocation, no world change).
- Chat message when key held: `[RorysExcavation] Excavated <count> extra blocks for id=<id> meta=<meta>`.

### Removed
- `ExcavationDetector.bfsFirstConnectedBlock` — superseded by `bfsCollectBlocks`. The v0.4.0 single-block path is replaced by the full-vein loop.

### Not yet implemented
- Tool damage, drops, blacklist.

---

## [0.4.0] — 2026-06-27

### Added
- **Activation-key gate**: excavation behavior (extra block breaking) only fires when the configured `activationKey` (default: Grave/tilde, LWJGL key 41) is held at the moment the player's block break is detected. Without the key the mod reports detection and BFS count only — no world modifications.
- `ExcavationDetector.WorldWriter` — minimal single-method interface (`setBlock`) that lets block-removal logic call into the obfuscated world without touching `xd` types directly. Implemented in `ExcavationHandler` using confirmed `xd.g(int,int,int,int)`.
- `ExcavationDetector.bfsFirstConnectedBlock` — returns the coordinates of the first BFS-reachable connected block (or `null`), used to select the one extra block to break.
- `ExcavationDetector.removeBlock(WorldWriter, x, y, z)` — thin wrapper that calls `writer.setBlock(x, y, z, 0)`, isolating world modification from BFS logic.
- `ExcavationDetector.seedQueue` extracted as a private helper to eliminate duplication between `bfsConnectedBlocks` and `bfsFirstConnectedBlock`.
- `ExcavationHandler` now calls `xd.g(x,y,z,0)` (confirmed as `World.setBlockWithNotify`) to remove the chosen block, triggering proper chunk dirty-marking and neighbor block-update notifications.
- New chat messages when key is held: `[RorysExcavation] Excavated 1 extra block id=<id> meta=<meta> pos=(x,y,z)`.
- Confirmed obfuscated mapping `xd.g(int,int,int,int)` = `setBlockWithNotify` documented in Core `docs/OBFUSCATION_MAP.md` (verified by `javap -c` bytecode inspection).

### Not yet implemented
- Breaking more than one extra block (full vein excavation planned for a future release).
- Tool damage, drops, blacklist.

---

## [0.3.0] — 2026-06-27

### Added
- `ExcavationDetector.WorldReader` — minimal two-method interface (`getBlockId`, `getBlockMeta`) that lets BFS logic query the world without touching obfuscated Minecraft types.
- `ExcavationDetector.bfsConnectedBlocks` — BFS traversal (not recursive DFS) that starts from the 6 immediate neighbors of the broken block, expands face-adjacently, matches by block ID and metadata, and caps at `maxBlocks` (default 64). Uses `HashSet<Long>` for visited tracking via a packed-long position key.
- `ExcavationHandler` now creates an anonymous `WorldReader` that delegates to the confirmed `xd.a` / `xd.e` world methods and passes it to BFS after each detected break.
- New in-game chat message after each break: `[RorysExcavation] Found <count> connected blocks for id=<id> meta=<meta>`.

### Not yet implemented
- No blocks are broken (debug-only release).
- No tool damage, no drops, no blacklist.

---

## [0.2.0] — 2026-06-27

### Added
- `ExcavationDetector.countMatchingNeighbors` — pure logic method that counts how many of the 6 face-adjacent blocks share the same block ID and metadata as the broken block.
- `ExcavationHandler` now reads the 6 orthogonal neighbor positions (+X, -X, +Y, -Y, +Z, -Z) after each detected break using the confirmed `xd.a` / `xd.e` world methods.
- New in-game chat message after each break: `[RorysExcavation] Found <n> matching neighbors for id=<id> meta=<meta>`.

### Not yet implemented
- BFS traversal or actual block breaking (planned for a future release).

---

## [0.1.0] — 2026-06-27

### Added
- `ExcavationHandler` — GAME-tick handler that polls `Minecraft.objectMouseOver` (runtime: `mc.z`) to detect when a targeted block disappears.
- `ExcavationDetector` — pure feature class (no Forge/LWJGL coupling) that logs detected breaks to the Minecraft console: block ID, metadata, and world coordinates.
- `ExcavationHandler` also sends an in-game chat message on each detected break via `GuiIngame.printChatMessage` (runtime: `mc.w.a(String)`), making detection immediately visible without needing an external log viewer.
- Block-break detection is gated by the `enableExcavation` config flag.
- Confirmed and documented runtime obfuscated mappings for `World.getBlockId` (`xd.a`), `World.getBlockMetadata` (`xd.d`), `MovingObjectPosition` (`pl`), `GuiIngame` (`aiy`), and `GuiIngame.printChatMessage` (`aiy.a`).

### Not yet implemented
- BFS traversal or multi-block breaking (planned for a future release).

---

## [0.0.1] — 2026-06-27

### Added
- Project scaffold: source layout, build instructions, documentation.
- `mod_RorysExcavation` entry point — mod loads and appears in the Forge/FML mod list.
- Config file created on first launch at `.minecraft/config/rorys-excavation.cfg`.
- Config properties defined: `enableExcavation`, `activationKey`, `maxBlocks`, `durabilityMode`.

### Not yet implemented
- Excavation behavior (planned for v0.1.0).
