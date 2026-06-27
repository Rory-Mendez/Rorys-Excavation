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
3. Select `rorys-excavation-0.0.1.jar`.
4. Click **Launch**. The mod loads automatically.

---

## Installation (manual)

1. Locate your `.minecraft` folder:
   - **Windows:** `%APPDATA%\.minecraft`
   - **macOS:** `~/Library/Application Support/minecraft`
   - **Linux:** `~/.minecraft`

2. Open the `mods/` folder inside it. Create it if it does not exist.

3. Drop `rorys-excavation-0.0.1.jar` into `mods/`.

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

Open it with any text editor to change settings:

```properties
# Enable or disable excavation entirely
enableExcavation=true

# LWJGL key code for the activation key. Default: 41 (Grave / tilde ~`)
activationKey=41

# Maximum number of connected blocks to break in one excavation action
maxBlocks=64

# If false (default), tool durability is deducted only once per excavation chain.
# If true, durability is deducted once for every block broken.
damagePerBlock=false
```

Common `activationKey` values:

| Key | Code |
|---|---|
| Grave / tilde (\`) | 41 |
| Left Control | 29 |
| Left Alt | 56 |
| Left Shift | 42 |
| Caps Lock | 58 |
| R | 19 |

---

## Uninstalling

Remove `rorys-excavation-0.0.1.jar` from the `mods/` folder. The config file at `.minecraft/config/rorys-excavation.cfg` is harmless to leave in place.
