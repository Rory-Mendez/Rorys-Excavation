# Changelog

All notable changes to Rory's Excavation are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning: [Semantic Versioning](https://semver.org/).

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
