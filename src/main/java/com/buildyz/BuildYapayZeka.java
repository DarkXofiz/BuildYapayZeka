package com.buildyz;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildYapayZeka extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private Library library;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File maps = new File(getDataFolder(), "maps");
        maps.mkdirs();
        library = new Library(maps, getLogger());
        for (String n : new String[]{"buildyapayzeka", "bot"}) {
            PluginCommand pc = getCommand(n);
            if (pc != null) {
                pc.setExecutor(this);
                pc.setTabCompleter(this);
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (UUID id : new ArrayList<>(sessions.keySet())) endSession(id);
    }

    int perTick() {
        return Math.max(100, getConfig().getInt("blocks-per-tick", 2500));
    }

    int gap() {
        return getConfig().getInt("structure-gap", 14);
    }

    void say(Player p, String msg) {
        p.sendMessage(Component.text("<BuildYapayZeka> ", NamedTextColor.AQUA).append(Component.text(msg, NamedTextColor.WHITE)));
    }

    void endSession(UUID id) {
        Session s = sessions.remove(id);
        if (s != null) s.stop();
    }

    // ------------------------------------------------------------------ komutlar

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player p) || !p.isOp()) {
            sender.sendMessage(Component.text("Bu komut sadece operatör oyuncular içindir.", NamedTextColor.RED));
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("bot")) {
            if (a.length == 0) {
                say(p, "Ne inşa edeyim? Örnek: /bot büyük spawn ve arena. Katalog için /bot liste.");
                return true;
            }
            handle(p, String.join(" ", a));
            return true;
        }
        String sub = a.length == 0 ? "" : Brain.fold(a[0]);
        if (sub.equals("aktif") || sub.equals("ac") || sub.equals("on")) {
            activate(p);
        } else if (sub.equals("kapat") || sub.equals("off")) {
            deactivate(p);
        } else {
            say(p, "Kullanım: /BuildYapayZeka aktif veya /BuildYapayZeka kapat");
        }
        return true;
    }

    private void activate(Player p) {
        if (sessions.containsKey(p.getUniqueId())) {
            say(p, "Zaten yanındayım.");
            return;
        }
        sessions.put(p.getUniqueId(), new Session(this, p));
        say(p, "Yanındayım. Chat'e ne inşa etmemi istediğini yaz. Örnek: büyük bir spawn yap. "
                + (Provider.detect(apiKey()) == null ? "API anahtarı yok, kendi ayrıştırıcımı kullanıyorum. "
                        : "Yazdığını " + Provider.detect(apiKey()).label + " ile anlıyorum. ")
                + "Kapatmak için /BuildYapayZeka kapat. Aktifken chat mesajların bana gider.");
    }

    private void deactivate(Player p) {
        if (!sessions.containsKey(p.getUniqueId())) {
            say(p, "Zaten kapalıyım.");
            return;
        }
        endSession(p.getUniqueId());
        p.sendMessage(Component.text("BuildYapayZeka kapandı. Chat normal çalışıyor.", NamedTextColor.GRAY));
    }

    // ------------------------------------------------------------------ chat

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!sessions.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Bukkit.getScheduler().runTask(this, () -> {
            String ft = Brain.fold(text).trim();
            boolean secret = ft.startsWith("anahtar ") || ft.startsWith("key ");
            p.sendMessage(Component.text("<" + p.getName() + "> " + (secret ? "anahtar ********" : text)));
            handle(p, text);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> sessions.values().forEach(Session::hide), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        endSession(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ komut işleme

    private void links(Player p) {
        for (Provider pv : Provider.values()) {
            p.sendMessage(Component.text((pv.free() ? "Ücretsiz katman: " : "Ücretli: ") + pv.label + "  ", NamedTextColor.GRAY)
                    .append(Component.text(pv.keyUrl, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(pv.keyUrl))));
        }
    }

    private static String mask(String k) {
        return k.length() > 12 ? k.substring(0, 7) + "****" + k.substring(k.length() - 4) : "****";
    }

    private String modelFor(Provider pv) {
        return getConfig().getString("models." + pv.id, pv.defaultModel);
    }

    /** /bot anahtar, /bot durum. Oturum açık olmasa da çalışır. */
    private boolean admin(Player p, String text) {
        String[] parts = text.trim().split("\\s+", 2);
        String first = Brain.fold(parts[0]);
        if (first.equals("anahtar") || first.equals("key")) {
            String arg = parts.length > 1 ? parts[1].trim() : "";
            if (arg.isEmpty()) {
                say(p, "Anahtarı aşağıdaki sitelerden alabilirsin. Sonra yaz: /bot anahtar <anahtar>. "
                        + "Türünü kendim tanırım: sk-ant (Claude), AIza (Gemini), gsk_ (Groq), sk-or (OpenRouter). Silmek için: /bot anahtar sil");
                links(p);
            } else if (Brain.fold(arg).equals("sil")) {
                getConfig().set("anthropic-api-key", "");
                saveConfig();
                say(p, "Anahtar silindi. Ortam değişkeni yoksa yerel ayrıştırıcıya geçtim.");
            } else {
                setKey(p, arg);
            }
            return true;
        }
        if (first.equals("durum") || first.equals("status")) {
            status(p);
            return true;
        }
        return false;
    }

    private void setKey(Player p, String key) {
        final Provider pv = Provider.detect(key);
        if (pv == null) {
            say(p, "Anahtar türünü tanıyamadım. Desteklenen anahtarlar sk-ant, AIza, gsk_ ve sk-or ile başlar.");
            links(p);
            return;
        }
        final String m = modelFor(pv);
        say(p, pv.label + " anahtarı kontrol ediliyor.");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String err = null;
            try {
                AiPlanner.check(pv, key, m);
            } catch (Exception e) {
                err = e.getMessage();
            }
            final String ferr = err;
            Bukkit.getScheduler().runTask(this, () -> {
                if (ferr == null) {
                    getConfig().set("anthropic-api-key", key);
                    saveConfig();
                    say(p, "Anahtar geçerli ve kaydedildi (" + mask(key) + "). Kullanılan yapay zeka: " + AiPlanner.describe(pv, m)
                            + ". Güvenlik için anahtarı sohbet geçmişinden ve konsol kaydından sildirmeyi unutma.");
                } else {
                    say(p, "Anahtar kabul edilmedi: " + ferr + ". Kaydedilmedi.");
                    links(p);
                }
            });
        });
    }

    private void status(Player p) {
        String key = apiKey();
        if (key.isBlank()) {
            say(p, "Anahtar yok. Şu an yazdıkların yerel kelime ayrıştırıcıyla anlaşılıyor, yapay zeka kullanılmıyor.");
            links(p);
            return;
        }
        final Provider pv = Provider.detect(key);
        if (pv == null) {
            say(p, "Kayıtlı anahtarın türü tanınmıyor (" + mask(key) + "). Yerel ayrıştırıcı kullanılıyor.");
            links(p);
            return;
        }
        final String m = modelFor(pv);
        String cfg = getConfig().getString("anthropic-api-key", "");
        say(p, "Anahtar var (" + mask(key) + ", kaynak: " + (cfg == null || cfg.isBlank() ? "ortam değişkeni" : "config")
                + "). Yapay zeka: " + AiPlanner.describe(pv, m) + (pv.free() ? ", ücretsiz katman" : "") + ". Doğruluyorum.");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String err = null;
            try {
                AiPlanner.check(pv, key, m);
            } catch (Exception e) {
                err = e.getMessage();
            }
            final String ferr = err;
            Bukkit.getScheduler().runTask(this, () -> {
                if (ferr == null) say(p, "Anahtar çalışıyor. Yazdıkların " + pv.label + " ile anlaşılıyor.");
                else say(p, "Anahtar çalışmıyor: " + ferr + ". Şu an yerel ayrıştırıcıya düşüyorum.");
            });
        });
    }

    private void handle(Player p, String text) {
        if (admin(p, text)) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null) {
            say(p, "Önce /BuildYapayZeka aktif yaz.");
            return;
        }
        String f = Brain.fold(text).trim();
        String[] tok = f.split("[^a-z0-9]+");
        String first = tok.length > 0 ? tok[0] : "";
        if (first.equals("iptal") || first.equals("cancel") || first.equals("dur") || first.equals("stop")) {
            s.cancel();
            say(p, "İnşa durduruldu. Kurulanları silmek için geri yaz.");
            return;
        }
        if (first.equals("geri") || first.equals("undo")) {
            s.undo(p);
            return;
        }
        if (first.equals("liste") || first.equals("list") || first.equals("katalog")) {
            int page = 1;
            for (String t : tok) {
                if (t.matches("\\d{1,3}")) page = Integer.parseInt(t);
            }
            list(p, page);
            return;
        }
        if (first.equals("yardim") || first.equals("help") || f.isEmpty()) {
            help(p);
            return;
        }
        String key = apiKey();
        final Provider pv = key.isBlank() ? null : Provider.detect(key);
        if (pv == null || Brain.any(tok, "harita map schem schematic")) {
            localPlan(p, s, text);
            return;
        }
        say(p, "Düşünüyorum.");
        final String model = modelFor(pv);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Brain.Plan result = null;
            String err = null;
            try {
                result = AiPlanner.plan(pv, key, model, text);
            } catch (Exception e) {
                err = e.getMessage();
            }
            final Brain.Plan fp = result;
            final String ferr = err;
            Bukkit.getScheduler().runTask(this, () -> {
                if (sessions.get(p.getUniqueId()) != s) return;
                if (fp == null) {
                    say(p, "Yapay zekaya ulaşamadım (" + ferr + "). Kendi ayrıştırıcımı kullanıyorum.");
                    localPlan(p, s, text);
                } else if (fp.items().isEmpty()) {
                    if (fp.summary().isBlank()) help(p);
                    else say(p, fp.summary());
                } else {
                    say(p, fp.summary());
                    s.enqueue(p, fp.items());
                }
            });
        });
    }

    private String apiKey() {
        String k = getConfig().getString("anthropic-api-key", "");
        if (k == null || k.isBlank()) k = System.getenv("ANTHROPIC_API_KEY");
        return k == null ? "" : k.trim();
    }

    private void localPlan(Player p, Session s, String text) {
        Brain.Plan plan = Brain.parse(text, library);
        if (plan.items().isEmpty()) {
            if (plan.summary().isBlank()) {
                say(p, "Bunu anlayamadım.");
                help(p);
            } else {
                say(p, plan.summary());
            }
            return;
        }
        say(p, plan.summary());
        s.enqueue(p, plan.items());
    }

    private void help(Player p) {
        say(p, "İstediğini düz yazıyla anlat. Örnek: 100x100 nether temalı spawn, 3 tane box pvp, büyük arena. "
                + "Türler: spawn, takas ve kasa, arena, box pvp, ffa çukuru, 1v1 odaları, sky pvp. "
                + "Hepsi veya sunucu yazarsan tüm paketi kurarım. Her seferinde farklı çıkar. "
                + "Komutlar: durum, anahtar, liste, iptal, geri, kapat.");
    }

    private void list(Player p, int page) {
        if (library.size() == 0) {
            say(p, "maps klasöründe .schem dosyası yok. Hazır şablon yok, yapılar yazdığına göre sıfırdan üretilir.");
            return;
        }
        int per = 15;
        int pages = (library.size() + per - 1) / per;
        page = Math.max(1, Math.min(page, pages));
        StringBuilder sb = new StringBuilder("Harita listesi sayfa " + page + "/" + pages + ". Kurmak için: harita <numara>");
        for (int i = (page - 1) * per; i < Math.min(library.size(), page * per); i++) {
            Library.Entry e = library.all().get(i);
            sb.append("\n#").append(e.id()).append(" ").append(e.name());
        }
        say(p, sb.toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length != 1) return out;
        String[] opts = cmd.getName().equalsIgnoreCase("bot")
                ? new String[]{"anahtar", "durum", "liste", "iptal", "geri", "yardim", "spawn", "arena", "boxpvp", "1v1", "takas", "sky", "hepsi"}
                : new String[]{"aktif", "kapat"};
        for (String o : opts) {
            if (o.startsWith(a[0].toLowerCase())) out.add(o);
        }
        return out;
    }
}
