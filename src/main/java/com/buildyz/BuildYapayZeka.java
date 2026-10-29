package com.buildyz;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
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
                + "Numara yazarsan hazır yapıyı kurarım (1 2 3 veya 10-20). Katalog: liste. "
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
            p.sendMessage(Component.text("<" + p.getName() + "> " + text));
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

    private void handle(Player p, String text) {
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
        Brain.Plan plan = Brain.parse(text, library);
        if (plan.items().isEmpty()) {
            say(p, "Bunu anlayamadım.");
            help(p);
            return;
        }
        say(p, plan.summary());
        s.enqueue(p, plan.items());
    }

    private void help(Player p) {
        say(p, "Şunları yazabilirsin: spawn, takas, kasa, arena, pvp, 1v1, sky. Boyut için büyük veya küçük. "
                + "Tema için nether, end, kuvars, deniz, çöl, bakır, sakura, blackstone. "
                + "Hepsi veya sunucu yazarsan tüm paketi kurarım. Numara yaz: 1 2 3 veya 100-110. "
                + "Rastgele 5 de çalışır. Komutlar: liste, iptal, geri, kapat.");
    }

    private void list(Player p, int page) {
        int per = 15;
        int pages = (library.size() + per - 1) / per;
        page = Math.max(1, Math.min(page, pages));
        StringBuilder sb = new StringBuilder("Katalog sayfa " + page + "/" + pages + ". Numarayı yaz, hemen kurayım.");
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
                ? new String[]{"liste", "iptal", "geri", "yardim", "spawn", "arena", "1v1", "takas", "sky", "hepsi", "rastgele"}
                : new String[]{"aktif", "kapat"};
        for (String o : opts) {
            if (o.startsWith(a[0].toLowerCase())) out.add(o);
        }
        return out;
    }
}
