package de.ruffy.client;

import java.util.List;

/** One independently switchable module shown in the Visuals control center. */
public record RuffyFeature(String key, String title, String description, Section section, Group group) {
    public enum Section {
        HUD("HUD", "Clean, transparent on-screen information"),
        MOTION("Motion", "Movement trails and kinetic effects"),
        ENTITIES("Entities", "Player and combat feedback visuals"),
        WORLD("World", "Atmosphere and environment effects");

        private final String title;
        private final String subtitle;

        Section(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String title() { return title; }
        public String subtitle() { return subtitle; }
    }

    public enum Group {
        GENERAL, ENTITIES, MOTION
    }

    public static final List<RuffyFeature> ALL = List.of(
            new RuffyFeature("watermark", "Watermark", "Compact Ruffy status tag", Section.HUD, Group.GENERAL),
            new RuffyFeature("fps", "Performance", "Live FPS counter", Section.HUD, Group.GENERAL),
            new RuffyFeature("coordinates", "Coordinates", "Player XYZ position", Section.HUD, Group.GENERAL),
            new RuffyFeature("keystrokes", "Keystrokes", "Minimal WASD input display", Section.HUD, Group.MOTION),
            new RuffyFeature("direction", "Direction", "Current compass direction", Section.HUD, Group.GENERAL),
            new RuffyFeature("speed", "Movement Speed", "Horizontal blocks per second", Section.HUD, Group.MOTION),
            new RuffyFeature("world_clock", "World Clock", "In-game time in a compact HUD chip", Section.HUD, Group.GENERAL),

            new RuffyFeature("nebula_particles", "Ambient Particles", "Soft purple particles around you", Section.MOTION, Group.MOTION),
            new RuffyFeature("orbit_particles", "Orbit Shards", "Slow particles orbiting your player", Section.MOTION, Group.MOTION),
            new RuffyFeature("movement_trail", "Movement Trail", "Subtle light trail while moving", Section.MOTION, Group.MOTION),
            new RuffyFeature("jump_burst", "Jump Burst", "Energy burst when leaving the ground", Section.MOTION, Group.MOTION),
            new RuffyFeature("sprint_dust", "Sprint Dust", "Soft dust puffs while sprinting", Section.MOTION, Group.MOTION),
            new RuffyFeature("landing_ring", "Landing Ring", "Energy ring when touching down", Section.MOTION, Group.MOTION),
            new RuffyFeature("step_sparks", "Footstep Sparks", "Small stepside sparks while walking", Section.MOTION, Group.MOTION),
            new RuffyFeature("starfall", "Starfall", "A faint falling-star particle effect", Section.MOTION, Group.MOTION),

            new RuffyFeature("hit_sparks", "Impact Slash", "Sweep and sparks on each hand swing", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("swing_animation", "Smooth Swing", "Refined first-person swing timing", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("critical_sparks", "Air Sparks", "Light particles while descending", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("low_health_aura", "Low Health Aura", "Warning aura when health is critical", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("potion_glow", "Potion Glow", "Enchant particles while effects are active", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("damage_pulse", "Damage Pulse", "A brief visual pulse after taking damage", Section.ENTITIES, Group.ENTITIES),

            new RuffyFeature("underwater_bubbles", "Water Bubbles", "Bubbles around you underwater", Section.WORLD, Group.MOTION),
            new RuffyFeature("weather_fx", "Weather FX", "Local rain particles in rainy biomes", Section.WORLD, Group.MOTION),
            new RuffyFeature("cave_dust", "Cave Dust", "Ambient dust in very dark areas", Section.WORLD, Group.MOTION)
    );
}
