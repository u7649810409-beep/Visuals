# Ruffy Visuals 1.7.0 — Crimson Red / Rounded 3D (Minecraft 1.21.11)

## Install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the `ruffy-visuals-1.21.11` artifact and extract it.
4. Put `ruffy-visuals-1.7.0.jar` into `.minecraft/mods`.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. Press **Right Shift** in a world to open the compact Crimson control center.

The **Style** tab contains 10 palettes, HUD/menu opacity, effect quality, Smooth Animations and the Smoothness control. The modules browser includes 3D Prism Core, 3D Orbit Drones and 3D Slash Volume.

World visuals are simulated by Ruffy's own Java animation engine and drawn as custom geometry, not spawned as vanilla particles.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run:

```bash
gradle build
```

The JAR is generated in `build/libs/`.
