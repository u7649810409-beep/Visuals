# Ruffy Visuals 1.5.0 (Minecraft 1.21.11)

## Download and install

1. Open the repository's **Actions** tab.
2. Select the latest successful **Ruffy Visuals Build** run.
3. Download the `ruffy-visuals-1.21.11` artifact and extract it.
4. Put `ruffy-visuals-1.5.0.jar` into `.minecraft/mods`.
5. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
6. Press **Right Shift** in a world to open the control center.

Open **Style** to find **Smooth Animations** and the **Smoothness** slider. Smoothness 0 disables easing; higher values make transitions softer and longer. The global switch affects the menu, HUD, key indicators and first-person swing rendering.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run:

```bash
gradle build
```

The JAR is generated in `build/libs/`.
