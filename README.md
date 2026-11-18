# Ruffy Visuals 1.5.0

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Smooth animation system

A shared, frame-rate-independent animation engine now drives the visual interface:

- Global **Smooth Animations** toggle in the **Style** tab.
- **Smoothness** slider from 0–100%; higher values use longer, softer easing, while 0 snaps immediately.
- Smooth menu entrance, selected tabs, hover states and module-row highlights.
- Animated on/off switches with sliding knobs and violet glow.
- Smooth palette selection and animated slider positions.
- HUD chips fade in, the health bar glides between values and WASD/Jump/Sprint indicators ease between pressed and released states.
- First-person bare-arm and held-item swing use the same smoothness value.

When Smooth Animations is switched off, UI transitions and the custom swing easing snap back to their direct states. Minecraft's native particle engine continues to handle particle motion and lifetimes.

## Violet Neon Control Center

- Compact charcoal-and-violet launcher inspired by the supplied reference image.
- Seven tabs: **Visuals, Presets, World, HUD, Style, Configs and Setup**.
- Searchable modules, All/Entities/Motion/Starred filters, favorites and individual toggles.
- Dedicated module settings inspector.
- Open with **Right Shift**.

## Color and HUD

Six coordinated palettes: Nebula (violet), Orchid, Emerald, Solar, Arctic and Mono.

The HUD uses translucent charcoal chips, palette-matched outlines, a brand badge, FPS, coordinates, direction, speed, world clock, optional health and armor displays, plus an animated compact input panel.

## Visual modules (27)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

**Motion:** Ambient Particles, Orbit Shards, Movement Trail, Jump Burst, Sprint Dust, Landing Ring, Footstep Sparks, Starfall and Palette Sparks.

**Entities:** Impact Slash, Smooth Swing, Air Sparks, Low Health Aura, Potion Glow and Damage Pulse.

**World:** Water Bubbles, Weather FX and Cave Dust.

The effects engine has Low, Medium and High quality levels. Settings are saved to `config/ruffyclient.properties`.

## Combat and compatibility

Smooth Swing changes only the local first-person rendering curve. Impact Slash and particles are cosmetic. Attack cooldowns, hit registration, reach, damage, packets and server-side behaviour are not modified.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds and verifies the remapped JAR with Java 21 and Gradle 9.2.1. Output: `build/libs/ruffy-visuals-1.5.0.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.5.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.
