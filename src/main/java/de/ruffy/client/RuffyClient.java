package de.ruffy.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Der Hintergrund kommt aus den Assets dieses Mods:
 * assets/minecraft/textures/gui/title/background/panorama_0..5.png
 * ueberschreiben das Panorama des Hauptmenues. Dafuer ist kein Mixin noetig,
 * deshalb bricht der Mod bei Minecraft-Updates nicht so schnell.
 *
 * Hier koennen spaeter weitere Features rein (HUD, Menues, ...).
 */
public class RuffyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ruffyclient");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Ruffy Client geladen");
    }
}
