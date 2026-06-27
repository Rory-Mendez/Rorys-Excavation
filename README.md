# Rory's Excavation

A lightweight Minecraft 1.2.5 quality-of-life mod inspired by FTB Ultimine / VeinMiner behavior.

**Latest version:** v0.2.0  
**Target:** Minecraft 1.2.5 · Forge 3.4.9.171 · FML 2.2.106.176

---

Part of the **Rory's Mods** ecosystem.

- Rorys-Utilities
- Rorys-Excavation
- Rorys-Mod-Core

---

## What it does

When the player holds an activation key and breaks a block, Rory's Excavation finds all connected blocks of the same type and breaks up to a configurable maximum in one action — no more mining one ore at a time.

**v0.2.0** extends break detection with a neighbor scan: after detecting a break the mod inspects all 6 face-adjacent blocks, counts how many share the same block ID and metadata, and reports the result in chat. No gameplay changes yet — actual vein breaking ships in a future release.

---

## Requirements

- Minecraft **1.2.5**
- **Minecraft Forge 3.4.9.171** installed (via Prism Launcher or a manual profile)
- Java 8

---

## Installation

See [docs/INSTALL.md](docs/INSTALL.md) for full instructions.

Short version: drop `rorys-excavation-0.0.1.jar` into your `mods/` folder and launch.

---

## Configuration

On first launch a config file is created at:

```e
.minecraft/config/rorys-excavation.cfg
```

Config options (effective from v0.1.0):

```properties
# Enable or disable excavation entirely
enableExcavation=true

# LWJGL key code for the activation key. Default: 41 (Grave / tilde)
activationKey=41

# Maximum number of connected blocks to break in one excavation action
maxBlocks=64

# If false (default), tool durability is deducted only once per excavation chain.
# If true, durability is deducted once for every block broken.
damagePerBlock=false
```

---

## Building from source

See [docs/BUILD.md](docs/BUILD.md).

---

## License

MIT — see [LICENSE](LICENSE).
