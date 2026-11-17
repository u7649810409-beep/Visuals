# Ruffy Visuals 1.4.0 (Minecraft 1.21.11)

## Download and install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the `ruffy-visuals-1.21.11` artifact and extract it.
4. Put `ruffy-visuals-1.4.0.jar` into `.minecraft/mods`.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. In a world, press **Right Shift** to open the violet neon control center.

Use **Visuals** to search and toggle modules, **Style** to select one of six palettes and adjust transparency/effect quality, and **HUD** to enable health and armor chips.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run:

```bash
gradle build
```

The JAR is generated in `build/libs/`.
