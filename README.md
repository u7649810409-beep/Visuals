# Ruffy Visuals 1.3.0

A standalone client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Compact control center

- Inventory-inspired compact window (responsive between 420–520 GUI pixels wide).
- Seven tabs: **Visuals, Presets, World, HUD, Style, Configs, Setup**.
- Search field reflows below the filters on narrow displays.
- Module browser with **All, Entities, Motion and Starred** filters.
- Eight modules per page, favorites, active switches and a right-side settings inspector.
- Four profiles: Nebula, Cinematic, Performance and Clean.
- Open with **Right Shift**.

## Color and transparency

Six coordinated palettes: Nebula, Orchid, Emerald, Solar, Arctic and Mono.

The Style tab controls HUD panel opacity, menu opacity and effect quality. HUD chips wrap on small screens and truncate long values instead of drawing off-screen. Jump and Sprint key states are shown in the compact keystrokes panel.

## Visual modules (25)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed and World Clock.

**Motion:** Ambient Particles, Orbit Shards, Movement Trail, Jump Burst, Sprint Dust, Landing Ring, Footstep Sparks, Starfall and Palette Sparks.

**Entities:** Impact Slash, Smooth Swing, Air Sparks, Low Health Aura, Potion Glow and Damage Pulse.

**World:** Water Bubbles, Weather FX and Cave Dust.

Palette Sparks use Minecraft's native dust particle options in the currently selected accent colors. The visual effect engine has Low, Medium and High quality levels, scaling spawn frequency and detail.

All modules are individually switchable. Settings are stored in `config/ruffyclient.properties`.

## Swing animation and safety

Smooth Swing adjusts only the local first-person rendering curve. Impact Slash and particles are cosmetic. Nothing changes attack cooldowns, hit registration, reach, damage, packets or server-side behaviour.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds the remapped JAR with Java 21 and Gradle 9.2.1. The distributable file is written to `build/libs/ruffy-visuals-1.3.0.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.3.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.
