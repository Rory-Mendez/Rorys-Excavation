# Architecture

Rory's Excavation is structured in horizontal layers. Each layer has one responsibility and depends only on layers below it.

---

## Layer Map

```e
┌──────────────────────────────────────────┐
│          mod_RorysExcavation.java        │  ← Forge entry point. Wires everything together.
└──────────┬───────────────────────────────┘
           │
     ┌─────┴──────────────────────────┐
     ▼                                ▼
 ModConfig                  ExcavationHandler      ← Tick handler; reads input and world state
                            GuiExcavationConfig    ← In-game settings screen (GuiScreen subclass)
                                     │
                                     ▼
                            ExcavationDetector     ← Pure BFS and world-modification logic
                                                      No Forge coupling. No obfuscated types.
```

---

## Packages

| Package | Contents |
| --- | --- |
| `(default)` | `mod_RorysExcavation`, `ExcavationHandler`, `GuiExcavationConfig`. Any class that directly touches obfuscated Minecraft runtime types must live here — Java does not allow named-package code to import default-package classes. |
| `com.rorysmod.excavation.config` | `ModConfig` — loads, caches, and persists all config values via Forge `Configuration`. |
| `com.rorysmod.excavation.feature` | `ExcavationDetector` — stateless BFS traversal and block-removal logic. No Forge, no LWJGL, no obfuscated types. |

---

## Default-package constraint

Obfuscated Minecraft game classes (`vq`, `xd`, `yw`, `vp`, `abp`, etc.) live in the default (unnamed) package. Java does not allow named-package code to import or reference default-package classes. Any code that touches obfuscated Minecraft types must therefore also live in the default package.

**Rule:** if a class needs to call a method or read a field on a raw Minecraft object, it belongs in the default package. Everything else belongs in `com.rorysmod.excavation.*`.

---

## Class responsibilities

### `mod_RorysExcavation` (default pkg)

ModLoader entry point. Discovered by ModLoader because it is named `mod_*` in the default package. On `load()`:

1. Resolves the config directory via `Loader.instance().getConfigDir()`.
2. Creates and loads `ModConfig`.
3. Registers `ExcavationHandler` as a GAME-tick handler.

### `ModConfig` (`com.rorysmod.excavation.config`)

Owns all configuration state. On `load()` it calls `forge.Configuration` to read (or create with defaults) each property, caches the parsed values, and holds the `Property` object references so that in-game edits can update `Property.value` and call `forge.save()` without reconstructing the configuration.

Properties: `enableExcavation`, `activationKey`, `maxBlocks`, `damagePerBlock`, `openConfigKey`, `debugMessages`, `blacklist`.

### `ExcavationHandler` (default pkg)

FML `ITickHandler` registered for `TickType.GAME`. On every game tick:

1. **Config screen key**: edge-detects the `openConfigKey` press and calls `mc.a(new GuiExcavationConfig(config))` = `displayGuiScreen`.
2. **Block-break detection**: polls `mc.f.a(prevX,prevY,prevZ)` = `getBlockId`. If the previously tracked block has become air, a break is detected.
3. **Blacklist gate**: if the broken block ID is in `config.getBlacklist()`, skips all excavation for this break.
4. **Excavation or count**: if the activation key is held, runs `ExcavationDetector.bfsCollectBlocks`, then for each collected position: calls `Block.dropBlockAsItemWithChance`, repositions newly spawned `EntityItem`s to the cluster center, removes the block via `setBlockWithNotify`, and applies tool durability. If the key is not held and `debugMessages=true`, runs `bfsConnectedBlocks` for a count-only report.
5. **Tracking update**: reads the new `objectMouseOver` target for the next tick.

### `GuiExcavationConfig` (default pkg)

Subclass of `vp` (GuiScreen). Opened by `ExcavationHandler` on the leading edge of the `openConfigKey` press. Displays six toggle/capture rows:

| Row | Control |
| --- | --- |
| Enable Excavation | boolean toggle |
| Activation Key | key-capture button |
| Max Blocks | `<` / `>` step buttons (1–512) |
| Damage Mode | boolean toggle (Per Chain / Per Block) |
| Debug Messages | boolean toggle |
| Open Config Key | key-capture button |

Changes are held as local edit fields and committed to `ModConfig` (and flushed to disk) only when the screen closes via `vp.e()` = `onGuiClosed`.

### `ExcavationDetector` (`com.rorysmod.excavation.feature`)

Stateless utility class. No Forge, no LWJGL, no obfuscated types.

- `bfsCollectBlocks` — BFS over all 26 neighbors (faces + edges + corners) matching by block ID and metadata, capped at `maxBlocks`. Returns positions for the caller to act on.
- `bfsConnectedBlocks` — Count-only variant; no position list allocation.
- `removeBlock` — Thin wrapper over `WorldWriter.setBlock(x,y,z,0)`.

The 26-neighbor search (vs. 6-face) is intentional: ore veins frequently connect only at edges or corners.

---

## Design Decisions

### Why is `ExcavationDetector` decoupled from Forge?

`ExcavationHandler` is a Forge `ITickHandler`. It must know about `TickType`, Minecraft, obfuscated world references, and LWJGL key state. If BFS logic lived inside it, it would be impossible to reason about independently and impossible to test without a running Minecraft instance.

`ExcavationDetector` receives only plain Java primitives and standard-library types through the `WorldReader` and `WorldWriter` interfaces. Every input is explicit.

### Why is `ModConfig` not a singleton?

Singletons with static mutable state are hard to reason about and can cause subtle order-of-initialisation bugs in the Forge lifecycle. `ModConfig` is created by the entry point during `load()` and injected into anything that needs it.

### Why store key codes as integers in the config?

LWJGL key codes are integers at runtime. Storing the integer avoids a string-to-int parse step and a map lookup. The comment written to the config file documents what key each common code corresponds to.

### Why BFS and not recursive DFS for block traversal?

Recursive DFS on large ore veins risks a stack overflow — Java's default thread stack is shallow. BFS with an explicit `ArrayDeque` is iterative, bounded by `maxBlocks`, and easier to interrupt mid-flight.

### Why does `GuiExcavationConfig` live in the default package?

It extends `vp` (GuiScreen) and creates `abp` (GuiButton) instances — both of which are obfuscated Minecraft default-package classes. Named-package Java code cannot reference them.

### Why does the settings screen not pause the game?

`doesGuiPauseGame()` (`vp.b()`) returns `false` so the world continues ticking while the screen is open. This ensures the player can use the screen while standing in a safe spot without freezing time around them.

---

## Adding a New Feature (future reference)

1. Create `src/main/java/com/rorysmod/excavation/feature/MyFeature.java` — pure logic, no Forge.
2. If it needs obfuscated MC types, create `src/main/java/MyFeatureHandler.java` in the default package; otherwise it can stay in the named package.
3. Register the handler in `mod_RorysExcavation.load()`.
4. Add any config keys to `ModConfig` and expose them in `GuiExcavationConfig` if they should be in-game editable.
