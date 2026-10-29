package com.buildyz;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bir operatöre bağlı bot: oyuncu görünümlü varlık, inşa kuyruğu ve geri alma. */
final class Session {

    record Undo(int x, int y, int z, BlockData old) {}

    private final BuildYapayZeka pl;
    private final UUID owner;
    private Entity bot;
    private final ArrayDeque<Brain.Item> queue = new ArrayDeque<>();
    private final ArrayList<Undo> undo = new ArrayList<>();
    private BukkitTask follow, work, undoTask;

    private Location cursor;
    private int rowStartX, rowDepth;

    private List<Canvas.Op> ops;
    private int idx, ox, oy, oz, total, done, tickCount;
    private World world, undoWorld;
    private Location firstSpawn;
    private String curName = "";

    Session(BuildYapayZeka pl, Player p) {
        this.pl = pl;
        this.owner = p.getUniqueId();
        spawnBot(p);
        follow = Bukkit.getScheduler().runTaskTimer(pl, this::followTick, 10L, 10L);
    }

    // ------------------------------------------------------------------ bot varlığı

    private void spawnBot(Player p) {
        Location l = p.getLocation().add(1.5, 0, 1.5);
        Entity e = null;
        try {
            e = l.getWorld().spawnEntity(l, EntityType.valueOf("MANNEQUIN"));
        } catch (Exception ex) {
            e = null;
        }
        if (e == null) {
            ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
            as.setArms(true);
            as.setBasePlate(false);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(p);
                head.setItemMeta(sm);
            }
            if (as.getEquipment() != null) as.getEquipment().setHelmet(head);
            e = as;
        } else {
            try {
                Class<?> m = Class.forName("org.bukkit.entity.Mannequin");
                try {
                    m.getMethod("setImmovable", boolean.class).invoke(e, true);
                } catch (Exception ignored) {
                    // eski API
                }
                try {
                    m.getMethod("setDescription", Component.class).invoke(e, (Object) null);
                } catch (Exception ignored) {
                    // eski API
                }
            } catch (Exception ignored) {
                // Mannequin sınıfı yok
            }
        }
        e.customName(Component.text("BuildYapayZeka", NamedTextColor.AQUA));
        e.setCustomNameVisible(true);
        e.setInvulnerable(true);
        e.setPersistent(false);
        e.setSilent(true);
        e.setGravity(false);
        bot = e;
        hide();
    }

    /** Bot yalnızca operatörlere görünür. */
    void hide() {
        if (bot == null || !bot.isValid()) return;
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.isOp() && o.canSee(bot)) o.hideEntity(pl, bot);
        }
    }

    private void followTick() {
        Player p = Bukkit.getPlayer(owner);
        if (p == null) {
            pl.endSession(owner);
            return;
        }
        if (bot == null || !bot.isValid()) spawnBot(p);
        hide();
        if (ops == null && (bot.getWorld() != p.getWorld() || bot.getLocation().distanceSquared(p.getLocation()) > 36)) {
            Location l = p.getLocation().add(1.5, 0, 1.5);
            l.setDirection(p.getLocation().toVector().subtract(l.toVector()));
            bot.teleport(l);
        }
    }

    // ------------------------------------------------------------------ inşa

    void enqueue(Player p, List<Brain.Item> items) {
        if (ops == null && queue.isEmpty()) {
            undo.clear();
            firstSpawn = null;
            total = 0;
            done = 0;
            Location pl2 = p.getLocation();
            if (cursor == null || cursor.getWorld() != pl2.getWorld() || cursor.distanceSquared(pl2) > 400.0 * 400.0) {
                cursor = pl2.clone().add(10, 0, 10);
                rowStartX = cursor.getBlockX();
                rowDepth = 0;
            }
        }
        queue.addAll(items);
        total += items.size();
        if (work == null) work = Bukkit.getScheduler().runTaskTimer(pl, this::tick, 1L, 1L);
    }

    private void tick() {
        Player p = Bukkit.getPlayer(owner);
        if (p == null) {
            pl.endSession(owner);
            return;
        }
        if (ops == null) {
            Brain.Item it = queue.poll();
            if (it == null) {
                finish(p);
                return;
            }
            begin(p, it);
            return;
        }
        int budget = pl.perTick();
        for (; budget > 0 && idx < ops.size(); budget--, idx++) {
            Canvas.Op o = ops.get(idx);
            int y = oy + o.y();
            if (y < world.getMinHeight() || y >= world.getMaxHeight()) continue;
            Block b = world.getBlockAt(ox + o.x(), y, oz + o.z());
            if (b.getBlockData().equals(o.data()) && o.post() == null) continue;
            if (undo.size() < 3_000_000) undo.add(new Undo(b.getX(), b.getY(), b.getZ(), b.getBlockData()));
            b.setBlockData(o.data(), false);
            if (o.post() != null) o.post().accept(b);
        }
        if (idx < ops.size()) {
            if (tickCount % 5 == 0 && bot != null && bot.isValid()) {
                Canvas.Op o = ops.get(idx);
                bot.teleport(new Location(world, ox + o.x() + 0.5, oy + o.y() + 2, oz + o.z() + 0.5));
                if (bot instanceof LivingEntity le) le.swingMainHand();
            }
            if (tickCount % 20 == 0) {
                int pct = (int) (100.0 * idx / ops.size());
                p.sendActionBar(Component.text("BuildYapayZeka: " + curName + " %" + pct + " (" + (done + 1) + "/" + total + ")", NamedTextColor.AQUA));
            }
            tickCount++;
        } else {
            pl.say(p, curName + " tamamlandı (" + (done + 1) + "/" + total + ").");
            ops = null;
            done++;
        }
    }

    private void begin(Player p, Brain.Item it) {
        Canvas c;
        try {
            c = it.make().get();
        } catch (Exception e) {
            pl.say(p, it.name() + " yüklenemedi: " + e.getMessage());
            done++;
            return;
        }
        world = cursor.getWorld();
        undoWorld = world;
        int w = c.width(), l = c.length();
        ox = cursor.getBlockX() - c.minX;
        oz = cursor.getBlockZ() - c.minZ;
        oy = world.getHighestBlockYAt(cursor.getBlockX() + w / 2, cursor.getBlockZ() + l / 2, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        ops = c.sorted();
        idx = 0;
        tickCount = 0;
        curName = it.name();
        if (firstSpawn == null) {
            firstSpawn = new Location(world, ox + c.spawnX() + 0.5, oy + c.spawnY(), oz + c.spawnZ() + 0.5);
        }
        cursor.add(w + pl.gap(), 0, 0);
        rowDepth = Math.max(rowDepth, l);
        if (cursor.getBlockX() - rowStartX > 420) {
            cursor.setX(rowStartX);
            cursor.add(0, 0, rowDepth + pl.gap());
            rowDepth = 0;
        }
        pl.say(p, "[" + (done + 1) + "/" + total + "] " + it.name() + " inşa ediliyor, " + ops.size() + " blok.");
    }

    private void finish(Player p) {
        if (work != null) {
            work.cancel();
            work = null;
        }
        if (firstSpawn != null) {
            p.teleport(firstSpawn);
            if (bot != null && bot.isValid()) bot.teleport(firstSpawn.clone().add(1.5, 0, 1.5));
            pl.say(p, "Hepsi hazır. Seni ilk yapıya ışınladım. Geri almak için geri yaz.");
        }
    }

    void cancel() {
        queue.clear();
        ops = null;
        if (work != null) {
            work.cancel();
            work = null;
        }
    }

    void undo(Player p) {
        if (undo.isEmpty() || undoWorld == null) {
            pl.say(p, "Geri alınacak inşa yok.");
            return;
        }
        cancel();
        final int[] pos = {undo.size() - 1};
        pl.say(p, undo.size() + " blok geri alınıyor.");
        undoTask = Bukkit.getScheduler().runTaskTimer(pl, () -> {
            int n = pl.perTick() * 2;
            for (; n > 0 && pos[0] >= 0; n--, pos[0]--) {
                Undo u = undo.get(pos[0]);
                undoWorld.getBlockAt(u.x(), u.y(), u.z()).setBlockData(u.old(), false);
            }
            if (pos[0] < 0) {
                undo.clear();
                undoTask.cancel();
                undoTask = null;
                pl.say(p, "Geri alma tamamlandı.");
            }
        }, 1L, 1L);
    }

    void stop() {
        cancel();
        if (undoTask != null) undoTask.cancel();
        if (follow != null) follow.cancel();
        if (bot != null) bot.remove();
        bot = null;
    }
}
