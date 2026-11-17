# Ruffy Visuals

A standalone, client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Futuristic visual suite

- Clean sci-fi ClickGUI with compact cards, cyan status accents and violet secondary highlights.
- **Right Shift** opens the control center.
- Persistent module toggles in `config/ruffyclient.properties`.
- Minimal HUD watermark, FPS counter, optional coordinates and WASD keystrokes.

## Motion and combat feedback

- **Smooth Swing** adjusts the first-person arm swing curve with a snappier ease-out animation.
- **Impact Slash** adds a sweep visual and electric sparks when the local player swings.
- **Air Sparks** adds subtle particles while descending.
- **Landing Ring** emits a compact energy ring on landing.
- **Sprint Dust**, **Movement Trail**, **Jump Burst** and **Nebula Particles** add optional movement cosmetics.

All effects are client-side cosmetics. The swing animation does not change attack cooldowns, hit registration, reach, damage, packets or server-side behavior.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds the remapped JAR with Java 21 and Gradle 9.2.1. The distributable file is written to `build/libs/ruffy-visuals-1.1.1.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.1.1.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile and press **Right Shift** in a world.

The feature toggles are saved automatically in the Ruffy Visuals config file.
