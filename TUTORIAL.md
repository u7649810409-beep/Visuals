# Ruffy Visuals 1.8.0 — Clean Crimson HUD (Minecraft 1.21.11)

## Install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the ruffy-visuals-1.21.11 artifact and extract it.
4. Put ruffy-visuals-1.8.0.jar into .minecraft/mods.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. Press **Right Shift** in a world to open the compact control center.

## HUD settings

Open **Style** to choose one of ten palettes, set HUD opacity, adjust HUD size (75–120%), tune menu opacity and effect quality, and configure Smooth Animations/Smoothness.

Open the **HUD** tab to toggle Watermark, FPS, Coordinates, Keystrokes, Direction, Speed, World Clock, Health and Armor independently.

The HUD uses compact telemetry cards and separate eased Health/Armor bars. Keystrokes are anchored near the lower-left corner.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run: gradle build

The JAR is generated in build/libs/.