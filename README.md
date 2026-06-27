# Rory's Excavation

A lightweight Minecraft 1.2.5 quality-of-life mod inspired by FTB Ultimine / VeinMiner behavior.

**Latest version:** v0.4.0  
**Target:** Minecraft 1.2.5 · Forge 3.4.9.171 · FML 2.2.106.176

---

Part of the **Rory's Mods** ecosystem.

- (Rorys-Utilities) [https://github.com/Rory-Mendez/Rorys-Utilities]
- (Rorys-Excavation) [https://github.com/Rory-Mendez/Rorys-Excavation]
- (Rorys-Mod-Core) [https://github.com/Rory-Mendez/Rorys-Mod-Core]

---

## What it does

When the player holds an activation key and breaks a block, Rory's Excavation finds all connected blocks of the same type and breaks up to a configurable maximum in one action — no more mining one ore at a time.

**v0.4.0** adds activation-key-gated excavation: hold the configured key (default: Grave/tilde `` ` ``) while breaking a block to trigger excavation. The mod runs BFS over connected matching blocks and breaks exactly one extra block — the nearest connected match. Without the key, only detection and BFS count are reported (no world changes). Full vein breaking ships in a future release.

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
