package com.buildyz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** Chat metnini Claude ile anlar ve yapı planına çevirir. Ağ çağrısı yapar, ana thread'de çağrılmamalı. */
final class AiPlanner {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String[] SIZE_NAMES = {"Küçük", "Orta", "Büyük", "Dev"};

    private static final String SYSTEM = """
            You turn Minecraft build requests from a server admin into a JSON build plan. Reply with ONLY one JSON object, no markdown, no extra text.
            Schema: {"reply": string, "builds": [{"kind": string, "size": int, "diameter": int, "theme": string, "count": int}]}
            kind is one of:
            SPAWN (round or square lobby hub with four gates, central monument, trade stalls, vault room),
            TRADE (trade plaza with trade booths, stalls and a hall of numbered chests),
            ARENA (PvP arena with tiered seats, tunnels, symmetric cover),
            BOXPVP (large glass box with floating platforms and pillars),
            PIT (FFA pit with ramps, pillars and water pools),
            DUELS (grid of enclosed 1v1 rooms),
            SKY (floating islands joined by bridges).
            size: 0 small, 1 medium, 2 large, 3 huge. diameter: target width in blocks (16 to 200), or 0 to let the builder choose randomly.
            theme: one of stone, deepslate, nether, end, quartz, prismarine, desert, copper, cherry, blackstone, forest, ice, tuff, neon, random.
            count: how many of that kind, 1 to 12.
            Rules: follow exactly what the admin wrote, including sizes, themes, counts and numbers. If the admin asks for a complete server or many things, return several builds with varied kinds and sizes. Never repeat the same combination twice in one plan, vary size and theme. If the message is not a build request, return an empty builds array and answer in reply.
            reply: one or two short sentences in the admin's language saying what you will build.
            """;

    private AiPlanner() {}

    private static JsonObject msg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static String explain(int c) {
        return switch (c) {
            case 400 -> "istek reddedildi, anahtar veya model adı hatalı olabilir";
            case 401 -> "anahtar geçersiz";
            case 403 -> "bu anahtarın yetkisi yok";
            case 404 -> "model bulunamadı";
            case 429 -> "limit aşıldı veya kredi bitti";
            default -> "HTTP " + c;
        };
    }

    /** Sağlayıcıya tek istek atar ve yanıt metnini döner. Hata olursa nedenini mesaj olarak fırlatır. */
    static String ask(Provider pv, String key, String model, String system, String user, int maxTokens) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        JsonArray msgs = new JsonArray();
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(pv.url))
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json");
        if (pv == Provider.ANTHROPIC) {
            if (!system.isEmpty()) body.addProperty("system", system);
            msgs.add(msg("user", user));
            b.header("x-api-key", key).header("anthropic-version", "2023-06-01");
        } else {
            if (!system.isEmpty()) msgs.add(msg("system", system));
            msgs.add(msg("user", user));
            b.header("Authorization", "Bearer " + key);
        }
        body.add("messages", msgs);
        HttpResponse<String> res = HTTP.send(b.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) throw new IOException(explain(res.statusCode()));
        JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
        if (pv == Provider.ANTHROPIC) {
            return root.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
        }
        JsonElement content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content");
        return content == null || content.isJsonNull() ? "" : content.getAsString();
    }

    /** Anahtarı ve modeli küçük bir istekle dener. */
    static void check(Provider pv, String key, String model) throws Exception {
        ask(pv, key, model, "", "hi", 16);
    }

    static String describe(Provider pv, String model) {
        if (pv == Provider.ANTHROPIC) return label(model) + " (" + model + ")";
        return pv.label + ", model " + model;
    }

    static Brain.Plan plan(Provider pv, String key, String model, String text) throws Exception {
        String out = ask(pv, key, model, SYSTEM, text, pv == Provider.ANTHROPIC ? 1024 : 2048);
        int a = out.indexOf('{'), b = out.lastIndexOf('}');
        if (a < 0 || b < a) throw new IOException("Geçersiz yanıt");
        JsonObject plan = JsonParser.parseString(out.substring(a, b + 1)).getAsJsonObject();

        String reply = plan.has("reply") ? plan.get("reply").getAsString() : "";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Theme shared = Theme.random(new Random(rnd.nextLong()));
        List<Brain.Item> items = new ArrayList<>();
        if (plan.has("builds") && plan.get("builds").isJsonArray()) {
            for (JsonElement el : plan.getAsJsonArray("builds")) {
                if (!el.isJsonObject() || items.size() >= 24) continue;
                JsonObject o = el.getAsJsonObject();
                Structures.Kind kind;
                try {
                    kind = Structures.Kind.valueOf(o.get("kind").getAsString().trim().toUpperCase());
                } catch (Exception e) {
                    continue;
                }
                int size = clamp(o, "size", 1, 0, 3);
                int dim = clamp(o, "diameter", 0, 0, 200);
                if (dim > 0 && dim < 16) dim = 16;
                int count = clamp(o, "count", 1, 1, 12);
                String themeKey = o.has("theme") ? Brain.fold(o.get("theme").getAsString()).trim() : "random";
                for (int i = 0; i < count && items.size() < 24; i++) {
                    final Structures.Kind fk = kind;
                    final int fsize = size, fdim = dim;
                    final long seed = rnd.nextLong();
                    final Theme theme = themeKey.equals("random") || themeKey.isEmpty()
                            ? shared : Theme.pick(new String[]{themeKey}, new Random(rnd.nextLong()));
                    String name = kind.label + " (" + theme.label + ", " + (fdim > 0 ? "~" + fdim + " blok" : SIZE_NAMES[fsize]) + ")";
                    items.add(new Brain.Item(name, () -> Structures.build(fk, theme, fsize, fdim, seed)));
                }
            }
        }
        return new Brain.Plan(items, reply);
    }

    static String label(String model) {
        if (model.contains("sonnet-5-5")) return "Claude Sonnet 5.5";
        if (model.contains("opus-5-5")) return "Claude Opus 5.5";
        if (model.contains("haiku-5-5")) return "Claude Haiku 5.5";
        if (model.contains("fable-5-1")) return "Claude Fable 5.1";
        return model;
    }

    private static int clamp(JsonObject o, String k, int def, int lo, int hi) {
        try {
            if (!o.has(k)) return def;
            return Math.max(lo, Math.min(hi, o.get(k).getAsInt()));
        } catch (Exception e) {
            return def;
        }
    }
}
