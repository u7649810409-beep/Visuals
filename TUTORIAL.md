# Ruffy Visuals 1.9.0 — Smooth Rounded Crimson (Minecraft 1.21.11)

## Install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the `ruffy-visuals-1.21.11` artifact and extract it.
4. Put `ruffy-visuals-1.9.0.jar` into `.minecraft/mods`.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. Press **Right Shift** in a world to open the compact client.

## Style settings

The Style tab contains 10 palettes, HUD opacity and scale, menu opacity, effect quality, Smooth Animations and Smoothness. Rounded GUI surfaces use an anti-aliased GPU mask, and the HUD shares the same renderer for its cards and input keys.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run `gradle build`. The JAR is generated in `build/libs/`.
