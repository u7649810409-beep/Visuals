package de.ruffy.client;

import java.util.List;

/** The settings exposed in the click GUI. Every entry maps to a live feature. */
public record RuffyFeature(String key, String title, String description, Category category) {
    public enum Category {
        HUD("HUD", "Information on your screen"),
        EFFECTS("Effects", "Movement, combat feedback and motion"),
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
            new RuffyFeature("nebula_particles", "Nebula Particles", "Soft ambient particles around you", Category.EFFECTS),
            new RuffyFeature("movement_trail", "Movement Trail", "Leave a subtle trail while moving", Category.EFFECTS),
            new RuffyFeature("jump_burst", "Jump Burst", "Emit a compact burst when jumping", Category.EFFECTS),
            new RuffyFeature("sprint_dust", "Sprint Dust", "Small dust puffs while sprinting", Category.EFFECTS),
            new RuffyFeature("landing_ring", "Landing Ring", "A clean energy ring on landing", Category.EFFECTS),
            new RuffyFeature("critical_sparks", "Air Sparks", "Light sparks while falling", Category.EFFECTS),
            new RuffyFeature("hit_sparks", "Impact Slash", "A short slash and sparks on each swing", Category.EFFECTS),
            new RuffyFeature("swing_animation", "Smooth Swing", "A sharper custom first-person swing curve", Category.EFFECTS)
    );
}
