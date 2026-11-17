# Ruffy Visuals

An original, client-side visuals mod for **Minecraft Java 1.21.11** using Fabric. It is built as a standalone mod, not a modified Minecraft client or a server component.

> The linked YouTube video could not be loaded in this environment (YouTube throttled the request), so this first implementation is an original visual-client foundation rather than a verified one-to-one reproduction. Provide a downloaded clip or screenshots to align the next iteration with the exact reference.

## Current features

- **Right Shift** opens the nebula-themed ClickGUI.
- Persistent feature toggles in `config/ruffyclient.properties`.
- HUD watermark, FPS counter, coordinates and WASD keystrokes.
- Ambient purple portal particles around the local player.
- Optional movement trail and jump-burst particles.
- Client-side only: no server plugin and no gameplay/combat automation.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.4 or newer**
- Fabric API **0.141.1+1.21.11**
- Java **21**

## Build

GitHub Actions builds the remapped JAR with Java 21 and Gradle 9.2.1. You can also build locally with Gradle 9.2.1 or newer:

```bash
gradle build
```

The distributable JAR is written to `build/libs/ruffy-visuals-1.0.0.jar` (Loom may also create a sources JAR).

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put the built `ruffy-visuals` JAR into `.minecraft/mods`.
4. Launch the Fabric 1.21.11 profile and press **Right Shift** in a world.

## Notes

The visual effects are cosmetic and local. No ESP, aim assistance, reach changes, packet manipulation, or other combat automation is included.
