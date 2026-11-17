# Ruffy Visuals (Minecraft 1.21.11)

## Build the mod with GitHub Actions

1. Open the repository's **Actions** tab.
2. Select the **Ruffy Visuals Build** workflow and open the latest successful run.
3. Download the `ruffy-visuals-1.21.11` artifact at the bottom of the run page.
4. Extract the downloaded artifact ZIP.
5. Put `ruffy-visuals-1.0.0.jar` into `.minecraft/mods`.
6. Start Minecraft 1.21.11 with Fabric Loader 0.18.4+ and Fabric API 0.141.1+1.21.11 installed.
7. In a world, press **Right Shift** to open the ClickGUI.

## Build locally

Install Java 21 and Gradle 9.2.1+, then run:

```bash
gradle build
```

The JAR is generated in `build/libs/`.
