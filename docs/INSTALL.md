# Installing Rory's Excavation

---

## Requirements

- Minecraft **1.2.5**
- **Minecraft Forge 3.4.9.171** installed (via Prism Launcher or a manual profile)
- Java 8

---

## Installation (Prism Launcher — recommended)

1. Open **Prism Launcher** and select your Minecraft 1.2.5 + Forge 3.4.9.171 instance.
2. Click **Edit** → **Mods** → **Add File**.
3. Select `rorys-excavation-1.0.0.jar`.
4. Click **Launch**. The mod loads automatically.

---

## Installation (manual)

1. Locate your `.minecraft` folder:
   - **Windows:** `%APPDATA%\.minecraft`
   - **macOS:** `~/Library/Application Support/minecraft`
   - **Linux:** `~/.minecraft`

2. Open the `mods/` folder inside it. Create it if it does not exist.

3. Drop `rorys-excavation-1.0.0.jar` into `mods/`.

4. Launch Minecraft with the Forge 3.4.9.171 profile.

---

## Why the `mods/` folder and not `minecraft.jar`?

Minecraft Forge 3.4.9.171 is a full mod loader. It patches Minecraft on startup and scans the `mods/` folder for jars to load. You do **not** need to patch `minecraft.jar` directly. Doing so would conflict with Forge's own patches and break other mods.

---

## Configuration

On first launch, a config file is created at:

```
.minecraft/config/rorys-excavation.cfg
```

You can edit this file with any text editor, or press **F12** in-game (while no other GUI is open) to open the settings screen and change most options there without restarting.

```properties
# Enable or disable excavation entirely. Toggleable in-game.
enableExcavation=true

# LWJGL key code for the excavation activation key. Default: 41 (Grave / tilde `).
# Hold this key while breaking a block to trigger excavation. Rebindable in-game.
activationKey=41

# Maximum number of connected blocks to break in one excavation action. Range: 1-512.
# Adjustable in-game.
maxBlocks=64

# If false (default), tool durability is deducted only once per excavation chain.
# If true, durability is deducted once for every extra block broken. Toggleable in-game.
damagePerBlock=false

# If true, excavation events are printed to in-game chat for troubleshooting.
# Disabled by default. Toggleable in-game.
debugMessages=false

# LWJGL key code to open the in-game settings screen. Default: 88 (F12).
# Rebindable in-game from the settings screen itself.
openConfigKey=88

# Comma-separated block IDs that are never excavated automatically.
# Manual breaking always works. Edit this file to change; not editable in-game.
blacklist=7,52,54,61,62,63,64,68,71
```

Common `activationKey` values:

| Key | Code |
|---|---|
| Grave / tilde (`` ` ``) | 41 |
| Left Control | 29 |
| Left Alt | 56 |
| Left Shift | 42 |
| Caps Lock | 58 |
| R | 19 |
| F | 33 |
| G | 34 |

Common `openConfigKey` values:

| Key | Code |
|---|---|
| F12 (default) | 88 |
| F11 | 87 |
| F10 | 68 |
| F9 | 67 |
| Insert | 210 |

---

## Default blacklist

The following block IDs are excluded from automatic excavation by default. You can add or remove IDs by editing the `blacklist` line in the config file.

| ID | Block | Reason |
|---|---|---|
| 7 | Bedrock | Indestructible; excavating it does nothing |
| 52 | Mob Spawner | Removing spawners silently is almost always unintentional |
| 54 | Chest | Destroying a Chest would lose its inventory contents |
| 61 | Furnace | Same inventory risk as Chest |
| 62 | Burning Furnace | Same risk; active state implies contents are present |
| 63 | Sign Post | Signs carry text; silent removal is surprising |
| 64 | Wooden Door | Half-block structure entity |
| 68 | Wall Sign | Same as Sign Post |
| 71 | Iron Door | Same as Wooden Door |

---

## Uninstalling

Remove `rorys-excavation-1.0.0.jar` from the `mods/` folder. The config file at `.minecraft/config/rorys-excavation.cfg` is harmless to leave in place.
