# Rory's Excavation

A lightweight Minecraft 1.2.5 quality-of-life mod inspired by FTB Ultimine / VeinMiner behavior.

**Latest version:** v1.0.0  
**Target:** Minecraft 1.2.5 · Forge 3.4.9.171 · FML 2.2.106.176

---

Part of the **Rory's Mods** ecosystem.

- (Rorys-Utilities) [https://github.com/Rory-Mendez/Rorys-Utilities]
- (Rorys-Excavation) [https://github.com/Rory-Mendez/Rorys-Excavation]
- (Rorys-Mod-Core) [https://github.com/Rory-Mendez/Rorys-Mod-Core]

---

## What it does

When the player holds an activation key and breaks a block, Rory's Excavation finds all connected blocks of the same type and breaks up to a configurable maximum in one action — no more mining one ore at a time.

Hold the configured key (default: Grave/tilde `` ` ``) while breaking a block and all face-adjacent connected blocks of the same type are broken in one action, up to the `maxBlocks` limit (default 64). All drops land tightly at the center of the original break position — no scatter. Extra blocks are harvested silently — no repeated break sounds or particles. Tool durability is consumed: once per chain (default) or once per extra block. Blacklisted blocks (Bedrock, Chests, Furnaces, Mob Spawners, Doors, and Signs by default) are never excavated automatically regardless of key state.

Press **F12** (default, rebindable) to open the in-game settings screen and change any option without editing the config file.

---

## Requirements

- Minecraft **1.2.5**
- **Minecraft Forge 3.4.9.171** installed (via Prism Launcher or a manual profile)
- Java 8

---

## Installation

See [docs/INSTALL.md](docs/INSTALL.md) for full instructions.

Short version: drop `rorys-excavation-1.0.0.jar` into your `mods/` folder and launch.

---

## Configuration

On first launch a config file is created at:

```e
.minecraft/config/rorys-excavation.cfg
```

Press **F12** in-game (no GUI open) to open the settings screen. The options marked **(GUI)** can be changed there without editing the file.

```properties
# Enable or disable excavation entirely. (GUI)
enableExcavation=true

# LWJGL key code for the excavation activation key. Default: 41 (Grave / tilde `).
# Hold this key while breaking a block to trigger excavation. (GUI)
activationKey=41

# Maximum number of connected blocks to break in one excavation action. Range: 1-512. (GUI)
maxBlocks=64

# If false (default), tool durability is deducted only once per excavation chain.
# If true, durability is deducted once for every block broken. (GUI)
damagePerBlock=false

# If true, debug messages are printed to in-game chat on each excavation event.
# Disabled by default — enable only for troubleshooting. (GUI)
debugMessages=false

# LWJGL key code to open the in-game settings screen. Default: 88 (F12).
# Change here or rebind in the settings screen itself. (GUI)
openConfigKey=88

# Comma-separated list of block IDs that are never excavated automatically.
# Manual breaking always works normally for these blocks.
# Edit this file to change the list; it is not editable in-game.
# Default covers dangerous or data-bearing blocks: Bedrock (7), Mob Spawner (52),
# Chest (54), Furnace (61), Burning Furnace (62), Sign Post (63),
# Wooden Door (64), Wall Sign (68), Iron Door (71).
blacklist=7,52,54,61,62,63,64,68,71
```

---

## Building from source

See [docs/BUILD.md](docs/BUILD.md).

---

## License

MIT — see [LICENSE](LICENSE).
