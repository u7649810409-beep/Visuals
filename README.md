# Ruffy Visuals 2.0.0 — Galaxy Edition

A client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Galaxy Control Center

- Fully redesigned Nebula / Galaxy visual identity with procedural purple-magenta nebula clouds and a star field.
- Larger translucent glass panel with large anti-aliased rounded corners.
- Soft violet/pink accent glow, pill navigation, roomy rounded module cards and animated toggles.
- Subtle animated star glints in UI space; these are GUI decorations, not Minecraft world particles.
- **10 palettes:** Nebula, Rose, Magenta, Violet, Azure, Ice, Mint, Emerald, Amber and Solar.
- Open with **Right Shift**.

## Rounded UI renderer

`RuffyRoundedUi` draws rounded buttons, cards, panels, pills and outlines using 4× supersampled alpha masks and a nine-slice GPU texture. The corner radius remains consistent when controls are stretched to different sizes.

## Smooth motion

`RuffyAnimation` uses a closed-form critically damped spring, configurable smoothness, easing curves and 3D vector rotation helpers. The system drives tabs, hover states, module cards, switches, HUD values and custom 3D models.

## Galaxy HUD

The HUD uses transparent violet-tinted glass cards with compact telemetry labels, FPS, heading, speed, world time and coordinates. Health and Armor have separate eased progress bars. The WASD / Jump / Sprint panel uses larger smooth keycaps and matching Nebula accents. HUD scale (75–120%) and opacity can be adjusted in Style.

## Custom 3D visuals

`RuffyVisualEffects` simulates and renders its own geometry through Fabric's world-render pass; it does not spawn Minecraft `ParticleTypes` or call `ClientLevel.addParticle`.

- 3D Prism Core: rotating octahedra, a wireframe cube and orbital loops.
- 3D Orbit Drones: three rotating crystal meshes.
- 3D Slash Volume: a curved swing ribbon with depth ribs.
- Custom rings, trails, shockwaves, glints, aura, helix, damage pulse, rain, cave glints, bubbles and starfall streaks.

## Modules

**HUD:** Watermark, Performance, Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

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

Run `gradle build`. GitHub Actions verifies `build/libs/ruffy-visuals-2.0.0.jar`, including the Galaxy background, rounded renderer, animation engine and custom 3D effects.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-2.0.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world.