# Ruffy Visuals 2.0.0 — Galaxy Edition (Minecraft 1.21.11)

## Install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the `ruffy-visuals-1.21.11` artifact and extract it.
4. Put `ruffy-visuals-2.0.0.jar` into `.minecraft/mods`.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. Press **Right Shift** in a world to open the Galaxy control center.

## Style

The Style tab contains ten palettes, HUD opacity and scale, menu opacity, effect quality, Smooth Animations and Smoothness. Nebula is the default. The full-screen galaxy background and all rounded panels are rendered by Ruffy's own client-side UI classes.

## Build

Install Java 21 and Gradle 9.2.1+, then run `gradle build`. The JAR is generated in `build/libs/`.