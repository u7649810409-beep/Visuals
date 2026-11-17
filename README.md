# Ruffy Visuals

A standalone, client-side visual mod for **Minecraft Java 1.21.11** using Fabric.

## Visual direction

The interface uses a restrained sci-fi control-center style: deep navy panels, fine cyan outlines, violet secondary accents, compact typography, clear active states and consistent spacing.

## Features

- **Right Shift** opens the futuristic ClickGUI.
- Three clean sections: HUD, Effects and About.
- Persistent feature toggles in `config/ruffyclient.properties`.
- Compact HUD watermark, FPS counter and optional coordinates.
- Minimal WASD keystrokes display with active key feedback.
- Optional ambient purple particles, movement trail and jump burst.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

```bash
gradle build
```

GitHub Actions builds the remapped JAR with Java 21 and Gradle 9.2.1. The distributable file is written to `build/libs/`.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put the built `ruffy-visuals` JAR into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile and press **Right Shift** in a world.

All toggles affect only local UI and cosmetic effects. No gameplay automation is included.
