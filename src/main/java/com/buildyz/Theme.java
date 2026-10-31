package com.buildyz;

import org.bukkit.Material;

import java.util.Random;

/** Her yapı için rastgele karıştırılmış blok paleti. Aile belirlenir, bloklar her seferinde rastgele seçilir. */
final class Theme {

    private record Family(String label, String keys, String base, String alt, String wall, String pillar,
                          String accent, String glass, String light) {}

    private static final Family[] FAMILIES = {
            new Family("Taş", "tas stone klasik kale castle",
                    "STONE_BRICKS,MOSSY_STONE_BRICKS,CRACKED_STONE_BRICKS", "POLISHED_ANDESITE,POLISHED_GRANITE,SMOOTH_STONE",
                    "STONE_BRICKS,POLISHED_ANDESITE,BRICKS", "CHISELED_STONE_BRICKS,POLISHED_DIORITE",
                    "MOSSY_COBBLESTONE,COBBLESTONE,GRAY_CONCRETE", "GLASS,LIGHT_GRAY_STAINED_GLASS", "GLOWSTONE,SEA_LANTERN,SHROOMLIGHT"),
            new Family("Derin Karataş", "deepslate derin karanlik dark",
                    "DEEPSLATE_BRICKS,DEEPSLATE_TILES,POLISHED_DEEPSLATE", "POLISHED_DEEPSLATE,COBBLED_DEEPSLATE",
                    "DEEPSLATE_TILES,DEEPSLATE_BRICKS", "CHISELED_DEEPSLATE,POLISHED_BLACKSTONE",
                    "COBBLED_DEEPSLATE,TUFF,CRYING_OBSIDIAN", "TINTED_GLASS,BLACK_STAINED_GLASS", "SEA_LANTERN,VERDANT_FROGLIGHT"),
            new Family("Nether", "nether cehennem lav",
                    "NETHER_BRICKS,RED_NETHER_BRICKS,CRACKED_NETHER_BRICKS", "RED_NETHER_BRICKS,BLACKSTONE,CRIMSON_PLANKS",
                    "NETHER_BRICKS,POLISHED_BLACKSTONE_BRICKS", "CHISELED_NETHER_BRICKS,CRIMSON_STEM",
                    "BLACKSTONE,CRYING_OBSIDIAN,WARPED_PLANKS", "RED_STAINED_GLASS,ORANGE_STAINED_GLASS", "SHROOMLIGHT,GLOWSTONE,OCHRE_FROGLIGHT"),
            new Family("End", "end uzay",
                    "END_STONE_BRICKS,PURPUR_BLOCK", "PURPUR_BLOCK,END_STONE", "END_STONE_BRICKS,PURPUR_BLOCK",
                    "PURPUR_PILLAR,OBSIDIAN", "OBSIDIAN,CRYING_OBSIDIAN,MAGENTA_CONCRETE",
                    "PURPLE_STAINED_GLASS,MAGENTA_STAINED_GLASS", "PEARLESCENT_FROGLIGHT,SEA_LANTERN"),
            new Family("Kuvars", "kuvars quartz beyaz modern white",
                    "QUARTZ_BRICKS,SMOOTH_QUARTZ,CALCITE", "SMOOTH_QUARTZ,WHITE_CONCRETE,DIORITE", "QUARTZ_BLOCK,WHITE_CONCRETE",
                    "QUARTZ_PILLAR,CHISELED_QUARTZ_BLOCK", "GOLD_BLOCK,LIGHT_GRAY_CONCRETE,BLACK_CONCRETE",
                    "WHITE_STAINED_GLASS,GLASS", "SEA_LANTERN,GLOWSTONE"),
            new Family("Deniz", "deniz prismarine okyanus ocean",
                    "PRISMARINE_BRICKS,PRISMARINE,DARK_PRISMARINE", "PRISMARINE,DARK_PRISMARINE", "PRISMARINE_BRICKS,PRISMARINE",
                    "DARK_PRISMARINE,PRISMARINE_BRICKS", "DARK_PRISMARINE,WARPED_PLANKS,CYAN_CONCRETE",
                    "CYAN_STAINED_GLASS,LIGHT_BLUE_STAINED_GLASS", "SEA_LANTERN"),
            new Family("Çöl", "desert col kum egypt misir sandstone",
                    "SMOOTH_SANDSTONE,CUT_SANDSTONE,SANDSTONE", "CUT_SANDSTONE,SMOOTH_RED_SANDSTONE", "SANDSTONE,CUT_SANDSTONE",
                    "CHISELED_SANDSTONE,CHISELED_RED_SANDSTONE", "RED_SANDSTONE,TERRACOTTA,ORANGE_TERRACOTTA",
                    "GLASS,YELLOW_STAINED_GLASS", "GLOWSTONE,OCHRE_FROGLIGHT"),
            new Family("Bakır", "bakir copper steampunk",
                    "CUT_COPPER,EXPOSED_CUT_COPPER,WEATHERED_CUT_COPPER", "COPPER_BLOCK,OXIDIZED_CUT_COPPER",
                    "WEATHERED_CUT_COPPER,CUT_COPPER", "CHISELED_COPPER,COPPER_BLOCK", "OXIDIZED_COPPER,WARPED_PLANKS",
                    "GLASS,CYAN_STAINED_GLASS", "OCHRE_FROGLIGHT,SHROOMLIGHT"),
            new Family("Sakura", "sakura kiraz cherry pembe pink japon",
                    "CHERRY_PLANKS,STRIPPED_CHERRY_LOG", "STRIPPED_CHERRY_LOG,PINK_TERRACOTTA", "CHERRY_PLANKS,WHITE_TERRACOTTA",
                    "CHERRY_LOG,STRIPPED_CHERRY_WOOD", "PINK_TERRACOTTA,PINK_CONCRETE,PINK_WOOL",
                    "PINK_STAINED_GLASS,WHITE_STAINED_GLASS", "PEARLESCENT_FROGLIGHT,GLOWSTONE"),
            new Family("Blackstone", "blackstone siyah black gece",
                    "POLISHED_BLACKSTONE_BRICKS,POLISHED_BLACKSTONE", "POLISHED_BLACKSTONE,BLACKSTONE",
                    "POLISHED_BLACKSTONE_BRICKS,BLACKSTONE", "CHISELED_POLISHED_BLACKSTONE,GILDED_BLACKSTONE",
                    "GILDED_BLACKSTONE,GOLD_BLOCK,CRYING_OBSIDIAN", "BLACK_STAINED_GLASS,TINTED_GLASS", "OCHRE_FROGLIGHT,SHROOMLIGHT"),
            new Family("Orman", "orman forest ahsap wood tahta",
                    "SPRUCE_PLANKS,DARK_OAK_PLANKS,STRIPPED_SPRUCE_LOG", "DARK_OAK_PLANKS,STRIPPED_DARK_OAK_LOG",
                    "SPRUCE_PLANKS,DARK_OAK_PLANKS", "DARK_OAK_LOG,SPRUCE_LOG", "MOSS_BLOCK,MUD_BRICKS,GREEN_TERRACOTTA",
                    "GLASS,GREEN_STAINED_GLASS", "SHROOMLIGHT,GLOWSTONE"),
            new Family("Buz", "buz ice kar snow kis winter",
                    "PACKED_ICE,WHITE_CONCRETE", "SNOW_BLOCK,LIGHT_BLUE_CONCRETE", "PACKED_ICE,CALCITE",
                    "BLUE_ICE,QUARTZ_PILLAR", "LIGHT_BLUE_CONCRETE,CYAN_CONCRETE", "LIGHT_BLUE_STAINED_GLASS,GLASS", "SEA_LANTERN"),
            new Family("Tuf", "tuff tuf antik ruins harabe",
                    "TUFF_BRICKS,POLISHED_TUFF,MUD_BRICKS", "POLISHED_TUFF,PACKED_MUD", "TUFF_BRICKS,MUD_BRICKS",
                    "CHISELED_TUFF,CHISELED_TUFF_BRICKS", "OXIDIZED_COPPER,MOSS_BLOCK", "GLASS", "OCHRE_FROGLIGHT,VERDANT_FROGLIGHT"),
            new Family("Neon", "neon cyber siber futuristik",
                    "BLACK_CONCRETE,GRAY_CONCRETE", "GRAY_CONCRETE,BLACKSTONE", "BLACK_CONCRETE,GRAY_CONCRETE",
                    "BLACK_CONCRETE,POLISHED_BLACKSTONE", "CYAN_CONCRETE,MAGENTA_CONCRETE,LIME_CONCRETE,ORANGE_CONCRETE",
                    "CYAN_STAINED_GLASS,MAGENTA_STAINED_GLASS,LIME_STAINED_GLASS", "SEA_LANTERN,PEARLESCENT_FROGLIGHT,VERDANT_FROGLIGHT")
    };

    final String label;
    final Material base, alt, wall, pillar, accent, glass, light;

    private Theme(String label, Material base, Material alt, Material wall, Material pillar,
                  Material accent, Material glass, Material light) {
        this.label = label;
        this.base = base;
        this.alt = alt;
        this.wall = wall;
        this.pillar = pillar;
        this.accent = accent;
        this.glass = glass;
        this.light = light;
    }

    static Theme random(Random r) {
        return of(FAMILIES[r.nextInt(FAMILIES.length)], r);
    }

    /** Yazıda tema kelimesi geçerse o aile, geçmezse tamamen rastgele aile. */
    static Theme pick(String[] tokens, Random r) {
        for (Family f : FAMILIES) {
            if (Brain.any(tokens, f.keys)) return of(f, r);
        }
        return random(r);
    }

    private static Theme of(Family f, Random r) {
        Family x = r.nextInt(3) == 0 ? FAMILIES[r.nextInt(FAMILIES.length)] : f;
        String label = x == f ? f.label : f.label + " + " + x.label;
        return new Theme(label, mat(f.base, r), mat(f.alt, r), mat(f.wall, r), mat(f.pillar, r),
                mat(x.accent, r), mat(f.glass, r), mat(f.light, r));
    }

    private static Material mat(String csv, Random r) {
        String[] a = csv.split(",");
        Material m = Material.matchMaterial(a[r.nextInt(a.length)]);
        return m == null ? Material.STONE : m;
    }
}
