# Ruffy Visuals 1.8.0 — Clean Crimson HUD

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Clean modern control center

- Compact 472 × 318 GUI-pixel layout, adapting to smaller screens.
- Rounded frame, navigation, module cards, action controls and pill switches.
- Simplified charcoal backdrop with softer accent outlines and less visual noise.
- **10 color palettes:** Crimson, Rose, Magenta, Violet, Azure, Ice, Mint, Emerald, Amber and Solar.
- Open with **Right Shift**.

## Redesigned HUD

The HUD is rebuilt as a compact telemetry strip with small uppercase labels and clear values. It uses a consistent height, subtle transparent surfaces and a restrained theme accent instead of thick neon rails.

- **Brand chip:** compact Ruffy identity tag.
- **Telemetry cards:** FPS, facing/heading, movement speed, world clock and XYZ position.
- **Vitals:** independently enabled Health and Armor cards with eased progress bars.
- **Movement panel:** smaller rounded WASD keys, Jump and Sprint states.
- **HUD scale:** adjustable from **75% to 120%** in the Style tab.
- **HUD opacity:** independently adjustable from the same tab.
- Only enabled HUD modules are rendered; overflow wraps to a second row.

The HUD uses its own GuiGraphics drawing and does not replace vanilla interface elements. The Style tab stores the chosen HUD scale in config/ruffyclient.properties.

## Rebuilt animation engine

RuffyAnimation uses a frame-rate-independent critically damped spring, configurable smoothness, cubic timing curves, shortest-path angle interpolation, color blending and 3D vector rotation helpers.

The engine animates GUI transitions, HUD values, health/armor meters and custom 3D geometry.

## Custom 3D visuals

RuffyVisualEffects owns its effect simulation and renders its own geometry in Fabric's world-render pass. It does not spawn Minecraft ParticleTypes or call ClientLevel.addParticle.

- **3D Prism Core:** two rotating octahedra, inner wireframe cube and orbital loops.
- **3D Orbit Drones:** three independent rotating crystal meshes following the player.
- **3D Slash Volume:** curved swing ribbon with depth and connecting ribs.
- Custom orbit rings, motion ribbons, jump/landing shockwaves, sprint streaks, glints, aura, helix, damage pulse, rain, cave glints, bubbles and starfall streaks.

3D rotations use monotonic real time; custom effect positions and lifetimes are interpolated between game ticks.

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

Run: gradle build

GitHub Actions builds and verifies build/libs/ruffy-visuals-1.8.0.jar with Java 21 and Gradle 9.2.1.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put ruffy-visuals-1.8.0.jar into .minecraft/mods.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.