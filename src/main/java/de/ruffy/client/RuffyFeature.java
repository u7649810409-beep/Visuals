package de.ruffy.client;

import java.util.List;

/** The settings exposed in the click GUI. Every entry maps to a live feature. */
public record RuffyFeature(String key, String title, String description, Category category) {
    public enum Category {
        HUD("HUD", "Information on your screen"),
        EFFECTS("Effects", "Small client-side visual effects"),
        ABOUT("About", "Client information and controls");

        private final String title;
        private final String subtitle;

        Category(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String title() { return title; }
        public String subtitle() { return subtitle; }
    }

    public static final List<RuffyFeature> ALL = List.of(
            new RuffyFeature("watermark", "Watermark", "Show the RUFFY VISUALS tag", Category.HUD),
            new RuffyFeature("fps", "FPS Counter", "Display the current frame rate", Category.HUD),
            new RuffyFeature("coordinates", "Coordinates", "Show your XYZ position", Category.HUD),
            new RuffyFeature("keystrokes", "Keystrokes", "Show pressed movement keys", Category.HUD),
            new RuffyFeature("nebula_particles", "Nebula Particles", "Drifting purple particles around you", Category.EFFECTS),
            new RuffyFeature("movement_trail", "Movement Trail", "Leave a short particle trail while moving", Category.EFFECTS),
            new RuffyFeature("jump_burst", "Jump Burst", "Emit a small ring when you jump", Category.EFFECTS)
    );
}
