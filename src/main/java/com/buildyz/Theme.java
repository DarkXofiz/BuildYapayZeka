package com.buildyz;

import org.bukkit.Material;

enum Theme {
    TAS("Taş", Material.STONE_BRICKS, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
            Material.MOSSY_STONE_BRICKS, Material.GLASS, Material.GLOWSTONE, "tas stone klasik kale castle"),
    KARA("Derin Karataş", Material.DEEPSLATE_BRICKS, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.CHISELED_DEEPSLATE,
            Material.COBBLED_DEEPSLATE, Material.TINTED_GLASS, Material.SEA_LANTERN, "deepslate derin karanlik dark"),
    NETHER("Nether", Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS, Material.CHISELED_NETHER_BRICKS,
            Material.BLACKSTONE, Material.RED_STAINED_GLASS, Material.SHROOMLIGHT, "nether cehennem lav"),
    END("End", Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS, Material.PURPUR_PILLAR,
            Material.OBSIDIAN, Material.PURPLE_STAINED_GLASS, Material.PEARLESCENT_FROGLIGHT, "end uzay"),
    KUVARS("Kuvars", Material.QUARTZ_BRICKS, Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK, Material.QUARTZ_PILLAR,
            Material.GOLD_BLOCK, Material.WHITE_STAINED_GLASS, Material.SEA_LANTERN, "kuvars quartz beyaz modern white"),
    DENIZ("Deniz", Material.PRISMARINE_BRICKS, Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE,
            Material.DARK_PRISMARINE, Material.CYAN_STAINED_GLASS, Material.SEA_LANTERN, "deniz prismarine okyanus ocean"),
    COL("Çöl", Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.SANDSTONE, Material.CHISELED_SANDSTONE,
            Material.RED_SANDSTONE, Material.GLASS, Material.GLOWSTONE, "desert col kum egypt misir sandstone"),
    BAKIR("Bakır", Material.CUT_COPPER, Material.EXPOSED_CUT_COPPER, Material.WEATHERED_CUT_COPPER, Material.CHISELED_COPPER,
            Material.OXIDIZED_CUT_COPPER, Material.GLASS, Material.OCHRE_FROGLIGHT, "bakir copper steampunk"),
    SAKURA("Sakura", Material.CHERRY_PLANKS, Material.STRIPPED_CHERRY_LOG, Material.CHERRY_PLANKS, Material.CHERRY_LOG,
            Material.PINK_TERRACOTTA, Material.PINK_STAINED_GLASS, Material.PEARLESCENT_FROGLIGHT, "sakura kiraz cherry pembe pink japon"),
    KARATAS("Blackstone", Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
            Material.CHISELED_POLISHED_BLACKSTONE, Material.GILDED_BLACKSTONE, Material.BLACK_STAINED_GLASS, Material.OCHRE_FROGLIGHT,
            "blackstone siyah black gece");

    final String label;
    final Material base, alt, wall, pillar, accent, glass, light;
    final String keys;

    Theme(String label, Material base, Material alt, Material wall, Material pillar, Material accent,
          Material glass, Material light, String keys) {
        this.label = label;
        this.base = base;
        this.alt = alt;
        this.wall = wall;
        this.pillar = pillar;
        this.accent = accent;
        this.glass = glass;
        this.light = light;
        this.keys = keys;
    }

    static Theme pick(String[] tokens) {
        for (Theme t : values()) {
            if (Brain.any(tokens, t.keys)) return t;
        }
        return null;
    }
}
