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
            new RuffyFeature("health_hud", "Health", "Compact health value and status bar", Section.HUD, Group.GENERAL),
            new RuffyFeature("armor_hud", "Armor", "Show equipped armor points", Section.HUD, Group.GENERAL),

            new RuffyFeature("nebula_particles", "Ambient Glints", "Custom drifting glow geometry around you", Section.MOTION, Group.MOTION),
            new RuffyFeature("orbit_particles", "Orbit Rings", "Custom animated geometry loops around your player", Section.MOTION, Group.MOTION),
            new RuffyFeature("movement_trail", "Motion Ribbon", "Custom velocity ribbon that follows your movement", Section.MOTION, Group.MOTION),
            new RuffyFeature("jump_burst", "Jump Shockwave", "Custom expanding rings and radial light geometry", Section.MOTION, Group.MOTION),
            new RuffyFeature("sprint_dust", "Sprint Streaks", "Custom streaks that trail behind your sprint", Section.MOTION, Group.MOTION),
            new RuffyFeature("landing_ring", "Landing Pulse", "Layered custom shock rings when landing", Section.MOTION, Group.MOTION),
            new RuffyFeature("step_sparks", "Footstep Glints", "Custom alternating glints at each step", Section.MOTION, Group.MOTION),
            new RuffyFeature("starfall", "Starfall Streaks", "Custom falling light streaks around you", Section.MOTION, Group.MOTION),
            new RuffyFeature("palette_sparks", "Palette FX", "Use selected Crimson palette colors in custom geometry", Section.MOTION, Group.MOTION),

            new RuffyFeature("hit_sparks", "Slash Arc", "Custom curved line geometry when you swing", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("swing_animation", "Smooth Swing", "Refined first-person swing timing", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("critical_sparks", "Air Streaks", "Custom velocity glints while descending", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("low_health_aura", "Critical Aura", "Custom rising helix when health is low", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("potion_glow", "Effect Orbit", "Custom double helix while potion effects are active", Section.ENTITIES, Group.ENTITIES),
            new RuffyFeature("damage_pulse", "Damage Pulse", "A brief visual pulse after taking damage", Section.ENTITIES, Group.ENTITIES),

            new RuffyFeature("underwater_bubbles", "Bubble Rings", "Custom rising outlined circles underwater", Section.WORLD, Group.MOTION),
            new RuffyFeature("weather_fx", "Rain Streaks", "Custom falling line geometry during rain", Section.WORLD, Group.MOTION),
            new RuffyFeature("cave_dust", "Cave Glints", "Custom drifting light geometry in caves", Section.WORLD, Group.MOTION)
    );
}
