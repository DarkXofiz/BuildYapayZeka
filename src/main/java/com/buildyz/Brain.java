package com.buildyz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Yazıyı anlar ve her seferinde farklı yapı planı üretir. Hazır şablon yok. */
final class Brain {

    record Item(String name, Supplier<Canvas> make) {}

    record Plan(List<Item> items, String summary) {}

    private static final Map<Structures.Kind, String> KEYS = new EnumMap<>(Structures.Kind.class);
    private static final List<Structures.Kind> PVP =
            List.of(Structures.Kind.ARENA, Structures.Kind.BOXPVP, Structures.Kind.SKY, Structures.Kind.PIT, Structures.Kind.DUELS);
    private static final String[] SIZE_NAMES = {"Küçük", "Orta", "Büyük", "Dev"};

    static {
        KEYS.put(Structures.Kind.SPAWN, "spawn dogma lobi lobby hub baslangic");
        KEYS.put(Structures.Kind.TRADE, "takas trade market pazar kasa vault ticaret sandik chest");
        KEYS.put(Structures.Kind.ARENA, "arena kolezyum coliseum colosseum stadyum stadium");
        KEYS.put(Structures.Kind.BOXPVP, "boxpvp box kutu cage kafes");
        KEYS.put(Structures.Kind.PIT, "cukur pit ffa");
        KEYS.put(Structures.Kind.DUELS, "1v1 1x1 duello duel oda odalar");
        KEYS.put(Structures.Kind.SKY, "sky skypvp gokyuzu ucan havada adalar");
    }

    private Brain() {}

    static String fold(String s) {
        String x = s.toLowerCase(Locale.ROOT).replace("\u0307", "");
        return x.replace('ı', 'i').replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c');
    }

    static boolean hit(String tok, String key) {
        return key.length() >= 4 ? tok.startsWith(key) : tok.equals(key);
    }

    static boolean any(String[] toks, String keys) {
        for (String t : toks) {
            for (String k : keys.split(" ")) {
                if (hit(t, k)) return true;
            }
        }
        return false;
    }

    private static int hitIndex(String[] tok, String keys) {
        for (int i = 0; i < tok.length; i++) {
            for (String k : keys.split(" ")) {
                if (hit(tok[i], k)) return i;
            }
        }
        return -1;
    }

    /** -1 kelime yok, 0 küçük, 1 orta, 2 büyük, 3 dev. */
    private static int sizeWord(String[] toks) {
        if (any(toks, "dev devasa mega huge muazzam")) return 3;
        if (any(toks, "buyuk large big genis")) return 2;
        if (any(toks, "kucuk small mini minik")) return 0;
        if (any(toks, "orta medium")) return 1;
        return -1;
    }

    /** 100x100, yarıçap 40 veya 80 blok gibi ifadelerden hedef çapı çıkarır. */
    private static int dimOf(String text) {
        Matcher m = Pattern.compile("(?<![a-z0-9])(\\d{2,3})\\s*x\\s*(\\d{2,3})(?![a-z0-9])").matcher(text);
        if (m.find()) return Math.min(200, Math.max(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
        m = Pattern.compile("(?:yaricap|radius)\\s*(\\d{2,3})").matcher(text);
        if (m.find()) return Math.min(200, Integer.parseInt(m.group(1)) * 2);
        m = Pattern.compile("(\\d{2,3})\\s*(?:blok|block)").matcher(text);
        if (m.find()) return Math.min(200, Integer.parseInt(m.group(1)));
        return 0;
    }

    private static int count(String f, Structures.Kind k) {
        StringBuilder alt = new StringBuilder();
        for (String key : KEYS.get(k).split(" ")) {
            if (Character.isDigit(key.charAt(0))) continue;
            if (alt.length() > 0) alt.append('|');
            alt.append(key);
        }
        Matcher m = Pattern.compile("(?<![a-z0-9])(\\d{1,2})\\s*(?:tane|adet|x)?\\s*(?:" + alt + ")").matcher(f);
        return m.find() ? Math.max(1, Math.min(12, Integer.parseInt(m.group(1)))) : 1;
    }

    static Plan parse(String raw, Library lib) {
        String f = fold(raw);
        String[] tok = f.split("[^a-z0-9]+");
        if (any(tok, "harita map schem schematic")) return mapMode(f, lib);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean all = f.contains("her sey") || any(tok, "hepsi hersey komple full tum sunucu server paket pack");
        Set<Structures.Kind> kinds = EnumSet.noneOf(Structures.Kind.class);
        for (Structures.Kind k : Structures.Kind.values()) {
            if (all || any(tok, KEYS.get(k))) kinds.add(k);
        }
        String note = "";
        if (any(tok, "pvp") && Collections.disjoint(kinds, PVP)) {
            List<Structures.Kind> pool = new ArrayList<>(PVP);
            Collections.shuffle(pool);
            kinds.addAll(pool.subList(0, 3));
            note = " PvP için üç tür rastgele seçtim.";
        }
        if (kinds.isEmpty()) {
            List<Structures.Kind> pool = new ArrayList<>(Arrays.asList(Structures.Kind.values()));
            Collections.shuffle(pool);
            kinds.addAll(pool.subList(0, 2 + rnd.nextInt(2)));
            note = " Net bir yapı adı bulamadım, rastgele türler seçtim.";
        }

        int gSize = sizeWord(tok);
        int gDim = dimOf(f);
        Theme theme = Theme.pick(tok, new Random(rnd.nextLong()));
        List<Item> items = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Structures.Kind k : Structures.Kind.values()) {
            if (!kinds.contains(k)) continue;
            int idx = hitIndex(tok, KEYS.get(k));
            int size = gSize, dim = gDim;
            if (idx >= 0) {
                String[] win = Arrays.copyOfRange(tok, Math.max(0, idx - 3), Math.min(tok.length, idx + 3));
                int ws = sizeWord(win);
                int wd = dimOf(String.join(" ", win));
                if (ws >= 0) size = ws;
                if (wd > 0) dim = wd;
            }
            int n = count(f, k);
            for (int i = 0; i < n; i++) {
                final Structures.Kind fk = k;
                final int fsize = size >= 0 ? size : 1 + rnd.nextInt(2);
                final int fdim = dim;
                final long seed = rnd.nextLong();
                String name = k.label + " (" + theme.label + ", " + (fdim > 0 ? "~" + fdim + " blok" : SIZE_NAMES[fsize]) + ")";
                items.add(new Item(name, () -> Structures.build(fk, theme, fsize, fdim, seed)));
                names.add(name);
            }
        }
        return new Plan(items, "Sıfırdan tasarlıyorum: " + String.join(", ", names) + "." + note + " İnşa başlıyor.");
    }

    private static Plan mapMode(String f, Library lib) {
        if (lib.size() == 0) {
            return new Plan(List.of(), "plugins/BuildYapayZeka/maps klasöründe .schem dosyası yok.");
        }
        List<Item> items = new ArrayList<>();
        Matcher sg = Pattern.compile("(?<![a-z0-9])(\\d{1,3})(?![a-z0-9])").matcher(f);
        while (sg.find()) {
            Library.Entry e = lib.get(Integer.parseInt(sg.group(1)));
            if (e != null) items.add(new Item("#" + e.id() + " " + e.name(), e.make()));
        }
        if (items.isEmpty()) return new Plan(items, "Harita numarası yaz. Liste için: liste");
        return new Plan(items, items.size() + " harita sıraya alındı. İnşa başlıyor.");
    }
}
