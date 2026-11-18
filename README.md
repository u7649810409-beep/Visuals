# Ruffy Visuals 1.7.0 — Crimson Red / Rounded 3D

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Crimson Control Center

The ClickGUI has a smaller 472 × 318 GUI-pixel target size (automatically adapts to smaller screens), with a dark charcoal/crimson skin:

- Custom rounded outer frame, rounded navigation tabs, search field, filters, module cards, preset cards, action buttons and pill switches.
- Animated on/off toggles and rounded hover surfaces.
- Compact module list with a right-hand settings inspector.
- **10 selectable palettes:** Crimson, Rose, Magenta, Violet, Azure, Ice, Mint, Emerald, Amber and Solar.
- Open with **Right Shift**.

## Rebuilt animation engine

`RuffyAnimation` is rewritten around a frame-rate-independent critically damped spring, with a configurable smoothness control. It includes custom cubic timing curves, shortest-path angle interpolation, color blending and 3D vector rotation helpers. The same engine drives GUI transitions, HUD state, and world geometry.

## Custom 3D effects — no vanilla particle calls

`RuffyVisualEffects` owns the animation state (position, velocity, gravity, damping, lifetime, shape and color) and submits its own geometry in Fabric's world-render pass. It does not call Minecraft's `ParticleTypes` or `ClientLevel.addParticle`.

3D visuals include:
- **3D Prism Core:** two interlocked rotating octahedra, a wireframe inner cube and three tilted orbital rings.
- **3D Orbit Drones:** three independent rotating crystal/octahedron meshes following the player.
- **3D Slash Volume:** a curved slash with parallel depth ribbons and connecting ribs.
- Smooth animated orbit rings, custom motion ribbons, jump/landing shockwaves, air glints, sprint streaks, critical aura, potion helix, damage pulse, rain streaks, cave glints, bubbles and starfall streaks.

3D mesh rotations use monotonic real time rather than game-tick-only stepping, so the models keep moving smoothly at high frame rates. Effect quality (Low/Medium/High) controls geometry density.

## HUD

The HUD now matches the rounded Crimson visual language: translucent charcoal status chips, rounded outlines, rounded key panels and eased WASD/Jump/Sprint states. It includes Ruffy branding, FPS, coordinates, direction, speed, world clock, optional health bar and armor.

## Modules (30)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

**Motion:** Ambient Glints, Orbit Rings, 3D Prism Core, 3D Orbit Drones, Motion Ribbon, Jump Shockwave, Sprint Streaks, Landing Pulse, Footstep Glints, Starfall Streaks and Palette FX.

**Entities:** Slash Arc, 3D Slash Volume, Smooth Swing, Air Streaks, Critical Aura, Effect Orbit and Damage Pulse.

**World:** Bubble Rings, Rain Streaks and Cave Glints.

Settings are saved in `config/ruffyclient.properties`.

## Smoothness and combat behaviour

The global **Smooth Animations** setting and **Smoothness** slider (0–100%) control GUI, HUD and custom 3D transitions. Smooth Swing only changes local first-person rendering; no attack cooldowns, reach, damage, hit registration, packets or server-side behaviour are altered.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds and verifies `build/libs/ruffy-visuals-1.7.0.jar` with Java 21 and Gradle 9.2.1.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.7.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.
