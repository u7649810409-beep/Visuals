# Ruffy Visuals 1.2.0

A standalone client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Control center

The ClickGUI is inspired by desktop visual-client panels:

- Seven top-level tabs: **Visuals, Presets, World, HUD, Style, Configs, Setup**.
- Searchable module browser with **All, Entities, Motion and Starred** filters.
- Two-column module list with favorites, active switches and a right-side inspector.
- Four quick profiles: Nebula, Cinematic, Performance and Clean.
- Keyboard shortcut: **Right Shift**.

## Multi-color style

Choose from six coordinated palettes:

- Nebula (cyan / violet)
- Orchid (pink / violet)
- Emerald (mint / cyan)
- Solar (amber / coral)
- Arctic (ice blue)
- Mono (silver)

The Style tab contains separate **HUD transparency** and **menu opacity** controls. The HUD uses translucent panels, color-matched outlines and compact information chips.

## Modules

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed and World Clock.

**Motion:** Ambient Particles, Orbit Shards, Movement Trail, Jump Burst, Sprint Dust, Landing Ring, Footstep Sparks and Starfall.

**Entities:** Impact Slash, Smooth Swing, Air Sparks, Low Health Aura, Potion Glow and Damage Pulse.

**World:** Water Bubbles, Weather FX and Cave Dust.

All effects are individually switchable and saved to `config/ruffyclient.properties`.

## Swing animation and safety

Smooth Swing adjusts only the local first-person rendering curve. Impact Slash and particles are cosmetic. Nothing changes attack cooldowns, hit registration, reach, damage, packets or server-side behavior.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds the remapped mod JAR with Java 21 and Gradle 9.2.1. The distributable file is written to `build/libs/ruffy-visuals-1.2.0.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.2.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world to open the control center.
