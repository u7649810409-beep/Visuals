# Ruffy Visuals 1.9.0 — Smooth Rounded Crimson

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Smooth anti-aliased control center

- Rounded controls now use a GPU-filtered 4× supersampled alpha mask and nine-slice texture rendering.
- Separate anti-aliased outline masks; no staircase-shaped pixel rectangles for rounded edges.
- Consistent curves on the main client frame, navigation tabs, module rows, search field, filters, buttons, switches and setting cards.
- Compact 472 × 318 GUI-pixel layout, adapting to smaller screens.
- Minimal charcoal / Crimson surface styling.
- **10 color palettes:** Crimson, Rose, Magenta, Violet, Azure, Ice, Mint, Emerald, Amber and Solar.
- Open with **Right Shift**.

## Smoother motion

`RuffyAnimation` now uses a closed-form critically damped spring rather than incremental Euler stepping. The spring preserves velocity, is stable across varying frame rates and settles without overshoot. It drives tab selection, hover transitions, row selection, sliders, switches, HUD values and custom 3D transitions.

## Clean modern HUD

The telemetry HUD uses compact metric cards with small labels and clear values:

- Ruffy brand chip, FPS, facing/heading, movement speed, world time and position.
- Separate Health and Armor cards with eased, rounded progress bars.
- Compact rounded WASD / Jump / Sprint movement panel.
- HUD scale adjustable from **75% to 120%** in Style.
- HUD opacity adjustable independently.
- Only enabled HUD modules are rendered; content wraps to a second row when required.

## Custom 3D visuals

`RuffyVisualEffects` owns its effect simulation and draws custom geometry through Fabric's world-render pass. It does not spawn Minecraft `ParticleTypes` or call `ClientLevel.addParticle`.

- 3D Prism Core: rotating octahedra, inner wireframe cube and orbit loops.
- 3D Orbit Drones: three independently rotating crystal meshes.
- 3D Slash Volume: curved swing ribbon with depth ribs.
- Custom trails, orbit rings, jump/landing shockwaves, glints, aura, helix, damage pulse, rain, cave glints, bubbles and starfall streaks.

## Modules (30)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

**Motion:** Ambient Glints, Orbit Rings, 3D Prism Core, 3D Orbit Drones, Motion Ribbon, Jump Shockwave, Sprint Streaks, Landing Pulse, Footstep Glints, Starfall Streaks and Palette FX.

**Entities:** Slash Arc, 3D Slash Volume, Smooth Swing, Air Streaks, Critical Aura, Effect Orbit and Damage Pulse.

**World:** Bubble Rings, Rain Streaks and Cave Glints.

## Compatibility

- Minecraft Java **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

Smooth Swing changes only local first-person rendering. Attack cooldowns, reach, damage, hit registration, packets and server-side behaviour are not modified.

## Build

Run `gradle build`. GitHub Actions verifies `build/libs/ruffy-visuals-1.9.0.jar`, including the rounded renderer, animation engine, 3D effects and Mixin metadata.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.9.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.
