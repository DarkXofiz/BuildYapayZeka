package com.buildyz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sohbet metnini yapı planına çevirir. Türkçe ve İngilizce anahtar kelimeleri anlar. */
final class Brain {

    record Item(String name, Supplier<Canvas> make) {}

    record Plan(List<Item> items, String summary) {}

    private static final Map<Structures.Kind, String> KEYS = new EnumMap<>(Structures.Kind.class);
    private static final Structures.Kind[] ORDER = {
            Structures.Kind.SPAWN, Structures.Kind.TRADE, Structures.Kind.ARENA, Structures.Kind.DUELS, Structures.Kind.SKY};
    private static final String[] SIZE_NAMES = {"Küçük", "Orta", "Büyük"};

    static {
        KEYS.put(Structures.Kind.SPAWN, "spawn dogma lobi lobby hub baslangic");
        KEYS.put(Structures.Kind.TRADE, "takas trade market pazar kasa vault ticaret sandik chest");
        KEYS.put(Structures.Kind.ARENA, "arena kolezyum coliseum colosseum ffa savas stadyum stadium pvp");
        KEYS.put(Structures.Kind.DUELS, "1v1 1x1 duello duel oda odalar");
        KEYS.put(Structures.Kind.SKY, "sky skypvp gokyuzu ucan havada adalar pvp");
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

    private static int count(String f, Structures.Kind k) {
        StringBuilder alt = new StringBuilder();
        for (String key : KEYS.get(k).split(" ")) {
            if (Character.isDigit(key.charAt(0))) continue;
            if (alt.length() > 0) alt.append('|');
            alt.append(key);
        }
        Matcher m = Pattern.compile("(?<![a-z0-9])(\\d{1,2})\\s*(?:tane|adet|x)?\\s*(?:" + alt + ")").matcher(f);
        return m.find() ? Math.max(1, Math.min(6, Integer.parseInt(m.group(1)))) : 1;
    }

    static Plan parse(String raw, Library lib) {
        String f = fold(raw);
        String[] tok = f.split("[^a-z0-9]+");
        boolean all = f.contains("her sey") || any(tok, "hepsi hersey komple full tum sunucu server paket pack");
        Set<Structures.Kind> kinds = EnumSet.noneOf(Structures.Kind.class);
        for (Map.Entry<Structures.Kind, String> e : KEYS.entrySet()) {
            if (all || any(tok, e.getValue())) kinds.add(e.getKey());
        }
        List<Item> items = new ArrayList<>();

        if (kinds.isEmpty()) {
            return libraryMode(f, tok, lib);
        }

        int size = any(tok, "buyuk large dev devasa mega huge genis big") ? 2
                : any(tok, "kucuk small mini minik") ? 0 : 1;
        Theme theme = Theme.pick(tok);
        if (theme == null) theme = Theme.values()[ThreadLocalRandom.current().nextInt(Theme.values().length)];
        List<String> names = new ArrayList<>();
        for (Structures.Kind k : ORDER) {
            if (!kinds.contains(k)) continue;
            int n = count(f, k);
            for (int i = 0; i < n; i++) {
                final Structures.Kind fk = k;
                final Theme fth = theme;
                final int fsize = size;
                final long seed = ThreadLocalRandom.current().nextLong();
                String name = k.label + " (" + theme.label + ", " + SIZE_NAMES[size] + ")";
                items.add(new Item(name, () -> Structures.build(fk, fth, fsize, seed)));
                names.add(name);
            }
        }
        return new Plan(items, "Planım: " + String.join(", ", names) + ". Her yapı benzersiz seed ile kurulur. İnşa başlıyor.");
    }

    private static Plan libraryMode(String f, String[] tok, Library lib) {
        List<Integer> ids = new ArrayList<>();
        Matcher rg = Pattern.compile("(?<![a-z0-9])(\\d{1,3})\\s*-\\s*(\\d{1,3})(?![a-z0-9])").matcher(f);
        while (rg.find()) {
            int a = Integer.parseInt(rg.group(1)), b = Integer.parseInt(rg.group(2));
            for (int i = Math.min(a, b); i <= Math.max(a, b) && ids.size() < 60; i++) ids.add(i);
        }
        String rest = rg.reset().replaceAll(" ");
        Matcher sg = Pattern.compile("(?<![a-z0-9])(\\d{1,3})(?![a-z0-9])").matcher(rest);
        while (sg.find() && ids.size() < 60) ids.add(Integer.parseInt(sg.group(1)));

        if (any(tok, "rastgele random")) {
            int n = ids.isEmpty() ? 3 : Math.min(ids.get(0), 20);
            List<Integer> pool = new ArrayList<>();
            for (int i = 1; i <= lib.size(); i++) pool.add(i);
            Collections.shuffle(pool);
            ids = new ArrayList<>(pool.subList(0, Math.min(n, pool.size())));
        }
        List<Item> items = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        for (int id : ids) {
            Library.Entry e = lib.get(id);
            if (e == null) {
                bad.add("#" + id);
                continue;
            }
            items.add(new Item("#" + e.id() + " " + e.name(), e.make()));
        }
        if (items.isEmpty()) return new Plan(items, "");
        String s = items.size() + " hazır yapı sıraya alındı.";
        if (!bad.isEmpty()) s += " Geçersiz numaralar atlandı: " + String.join(", ", bad) + ". Katalogda " + lib.size() + " yapı var.";
        return new Plan(items, s + " İnşa başlıyor.");
    }
}
