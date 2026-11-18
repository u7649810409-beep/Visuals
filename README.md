# Ruffy Visuals 1.6.0 — Crimson Red

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Crimson Red Control Center

The compact ClickGUI and HUD use a consistent dark-wine / charcoal surface with crimson neon borders, red active switches and matching red status accents.

- Seven tabs: **Visuals, Presets, World, HUD, Style, Configs and Setup**.
- Searchable module browser with **All, Entities, Motion and Starred** filters.
- Favorites, active toggles, animated rows and a dedicated module inspector.
- Six palettes: **Crimson, Orchid, Emerald, Solar, Arctic and Mono**.
- Open with **Right Shift**.

## Ruffy's own animation and render engine

World effects no longer call Minecraft's `ParticleTypes` or `ClientLevel.addParticle`.

The new `RuffyVisualEffects` engine stores and updates its own Java effect objects (position, velocity, damping, gravity, lifetime, shape and color), then draws custom line geometry through Fabric's world-render event. Effects include:

- Custom curved sword/hand slash arcs.
- Expanding jump and landing shock rings.
- Orbiting double-helix loops.
- Velocity-following movement ribbons and sprint streaks.
- Air glints, footstep glints, critical-health aura and potion-effect helix.
- Custom damage pulses, underwater outline rings, rain streaks, cave glints and starfall streaks.
- Primary/secondary colors follow the selected palette.

Rendering is submitted with Fabric's world-render API after entities are rendered. The visual engine uses Ruffy-owned simulation and geometry; it does not spawn vanilla Minecraft particles.

## Smooth animation controls

The **Style** tab has a global **Smooth Animations** switch and **Smoothness** slider (0–100%). They drive:

- Menu entrance, tab selection, module hover/selection and active switches.
- HUD chip opacity, health bar interpolation and WASD/Jump/Sprint states.
- Custom effect enable/disable fades and the first-person swing easing.

Low/Medium/High effect quality adjusts the amount and detail of custom geometry.

## HUD

The HUD follows the Crimson Red theme: translucent charcoal chips, fine crimson outlines, a Ruffy brand badge, FPS, coordinates, direction, speed, world clock, optional health/armor displays and an animated input panel.

## Visual modules (27)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

**Motion:** Ambient Glints, Orbit Rings, Motion Ribbon, Jump Shockwave, Sprint Streaks, Landing Pulse, Footstep Glints, Starfall Streaks and Palette FX.

**Entities:** Slash Arc, Smooth Swing, Air Streaks, Critical Aura, Effect Orbit and Damage Pulse.

**World:** Bubble Rings, Rain Streaks and Cave Glints.

Settings are saved to `config/ruffyclient.properties`.

## Combat and compatibility

The custom swing curve affects only local first-person rendering. Slash arcs and other effects are cosmetic. Attack cooldowns, hit registration, reach, damage, packets and server-side behaviour are not modified.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds and verifies the remapped JAR with Java 21 and Gradle 9.2.1. Output: `build/libs/ruffy-visuals-1.6.0.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.6.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.
