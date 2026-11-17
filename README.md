# Ruffy Visuals 1.4.0

A standalone client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Violet Neon Control Center

The compact ClickGUI is restyled as a charcoal-and-violet module launcher, inspired by the supplied visual reference:

- Dark charcoal panels, neon violet edge lighting and purple active switches.
- Seven tabs: **Visuals, Presets, World, HUD, Style, Configs and Setup**.
- Searchable module list with **All, Entities, Motion and Starred** filters.
- Module rows include a category icon, favorite star, settings/edit action and on/off switch.
- A dedicated settings inspector sits at the right side of the module browser.
- Compact window size: up to **520 × 340 GUI pixels**.
- Open with **Right Shift**.

## Color and transparency

Six coordinated palettes remain selectable: **Nebula (violet), Orchid, Emerald, Solar, Arctic and Mono**. The default palette is now violet/purple to match the reference.

The Style tab controls HUD panel transparency, menu opacity and effect quality. Palette Sparks use the selected primary/secondary colors in the actual Minecraft dust particles.

## HUD

The in-game HUD follows the same visual system:

- Semi-transparent charcoal information chips with violet/palette outlines.
- Ruffy Visuals brand badge, FPS, coordinates, direction, speed and world clock.
- Optional health bar and armor value.
- Compact WASD, Jump and Sprint input panel.
- Automatic line wrapping on narrow screens.

HUD modules are independently switchable.

## Visual modules (27)

**HUD:** Watermark, Performance (FPS), Coordinates, Keystrokes, Direction, Movement Speed, World Clock, Health and Armor.

**Motion:** Ambient Particles, Orbit Shards, Movement Trail, Jump Burst, Sprint Dust, Landing Ring, Footstep Sparks, Starfall and Palette Sparks.

**Entities:** Impact Slash, Smooth Swing, Air Sparks, Low Health Aura, Potion Glow and Damage Pulse.

**World:** Water Bubbles, Weather FX and Cave Dust.

The effects engine has Low, Medium and High quality levels, scaling particle frequency and detail. Settings are stored in `config/ruffyclient.properties`.

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

GitHub Actions builds and verifies the remapped JAR with Java 21 and Gradle 9.2.1. The distributable file is written to `build/libs/ruffy-visuals-1.4.0.jar`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put `ruffy-visuals-1.4.0.jar` into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile.
5. Press **Right Shift** in a world to open the Visuals control center.
