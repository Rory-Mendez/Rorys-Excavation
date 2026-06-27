# Architecture

Rory's Excavation is structured in horizontal layers. Each layer has one responsibility and depends only on layers below it.

---

## Layer Map

```
┌──────────────────────────────────────────┐
│          mod_RorysExcavation.java        │  ← Forge entry point. Wires everything together.
└─────────────────────┬────────────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
      ModConfig          ExcavationHandler   ← Config reader · tick/event bridge
                                  │
                                  ▼
                          ExcavationLogic    ← Pure feature logic. No Forge coupling. (v0.1.0)
```

---

## Packages

| Package | Purpose |
|---|---|
| `(default)` | `mod_RorysExcavation` entry point and any class that must directly access obfuscated Minecraft types. |
| `com.rorysmod.excavation.config` | Reads and exposes config values via Forge `Configuration`. |
| `com.rorysmod.excavation.handler` | Forge tick/event handlers. Reads input and world state, calls feature classes. |
| `com.rorysmod.excavation.feature` | One class per feature. Stateless logic only. No Forge or LWJGL coupling. |

---

## Default-package constraint

Obfuscated Minecraft game classes (`vq`, `xd`, `yw`, etc.) live in the default (unnamed) package.  
Java does not allow named-package code to import or reference default-package classes.  
Any code that touches obfuscated Minecraft types must therefore also live in the default package.

**Rule:** if a class needs to call a method or read a field on a raw Minecraft object, it belongs in the default package. Everything else belongs in `com.rorysmod.excavation.*`.

---

## Design Decisions

### Why separate `ExcavationLogic` from `ExcavationHandler`?

`ExcavationHandler` is a Forge `ITickHandler`. It must know about `TickType`, `Minecraft`, obfuscated world references, and LWJGL key state. If excavation logic lived inside it, it would be impossible to reason about independently.

`ExcavationLogic` will receive explicit primitives and plain Java types — no Forge, no LWJGL, no obfuscated references. Every input is explicit.

### Why is `ModConfig` not a singleton?

Singletons with static mutable state are hard to test and can cause subtle order-of-initialisation bugs in the Forge lifecycle. `ModConfig` is created by the entry point during `load()` and injected into anything that needs it. One instance, no static fields.

### Why store `activationKey` as an integer in the config?

LWJGL key codes are integers at runtime. Storing the integer avoids a parsing step and a map lookup on every tick. The comment in the config file documents what key each common code corresponds to.

### Why BFS and not recursive DFS for block traversal?

Recursive DFS on large ore veins risks a stack overflow — Java's default thread stack is shallow. BFS with an explicit queue is iterative, bounded by `maxBlocks`, and easier to interrupt mid-flight.

---

## Adding a New Feature (future reference)

1. Create `src/main/java/com/rorysmod/excavation/feature/MyFeature.java` — pure logic, no Forge.
2. Create `src/main/java/MyFeatureHandler.java` (default pkg) if it needs obfuscated MC types, otherwise `com.rorysmod.excavation.handler/MyFeatureHandler.java`.
3. Register the handler in `mod_RorysExcavation.load()`.
4. Add any config keys to `ModConfig`.
