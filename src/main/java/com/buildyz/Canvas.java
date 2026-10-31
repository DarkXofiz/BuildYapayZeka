package com.buildyz;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Dünyaya yazmadan önce yapının tüm blokları burada toplanır. Koordinatlar göreceli, y=0 zemin seviyesidir. */
final class Canvas {

    record Op(int x, int y, int z, BlockData data, Consumer<Block> post) {}

    private static final Map<Material, BlockData> CACHE = new EnumMap<>(Material.class);
    private final LinkedHashMap<Long, Op> ops = new LinkedHashMap<>();

    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private int sx, sy, sz;
    private boolean spawnSet;
    private int rot;

    /** Yapıyı 90 derece saat yönünde k kez döndürür. Hiçbir blok konmadan önce çağrılmalı. */
    void rotate(int k) { rot = Math.floorMod(k, 4); }

    private BlockFace rotFace(BlockFace f) {
        BlockFace[] ring = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (int i = 0; i < 4; i++) {
            if (ring[i] == f) return ring[(i + rot) % 4];
        }
        return f;
    }

    static BlockData data(Material m) {
        return CACHE.computeIfAbsent(m, Material::createBlockData);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 100000) << 40) | ((long) (z + 100000) << 16) | (long) (y + 20000);
    }

    void put(int x, int y, int z, BlockData d, Consumer<Block> post) {
        for (int i = 0; i < rot; i++) {
            int tmp = x;
            x = -z;
            z = tmp;
        }
        ops.put(key(x, y, z), new Op(x, y, z, d, post));
        if (!d.getMaterial().isAir()) {
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
    }

    void set(int x, int y, int z, Material m) {
        put(x, y, z, data(m), null);
    }

    void box(int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        BlockData d = data(m);
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    put(x, y, z, d, null);
    }

    void air(int x1, int y1, int z1, int x2, int y2, int z2) {
        box(x1, y1, z1, x2, y2, z2, Material.AIR);
    }

    void disc(int cx, int y, int cz, double r, Material m) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++)
            for (int dz = -ri; dz <= ri; dz++)
                if (dx * dx + dz * dz <= r * r) set(cx + dx, y, cz + dz, m);
    }

    void facing(int x, int y, int z, Material m, BlockFace f) {
        f = rotFace(f);
        BlockData d = Bukkit.createBlockData(m);
        if (d instanceof Directional dd) dd.setFacing(f);
        put(x, y, z, d, null);
    }

    private static Consumer<Block> text(String a, String b) {
        return blk -> {
            if (blk.getState() instanceof Sign s) {
                SignSide side = s.getSide(Side.FRONT);
                side.line(0, Component.text(a));
                side.line(1, Component.text(b));
                s.setWaxed(true);
                s.update(true, false);
            }
        };
    }

    void wallSign(int x, int y, int z, BlockFace f, String a, String b) {
        f = rotFace(f);
        BlockData d = Bukkit.createBlockData(Material.OAK_WALL_SIGN);
        if (d instanceof Directional dd) dd.setFacing(f);
        put(x, y, z, d, text(a, b));
    }

    void standSign(int x, int y, int z, BlockFace f, String a, String b) {
        f = rotFace(f);
        BlockData d = Bukkit.createBlockData(Material.OAK_SIGN);
        if (d instanceof Rotatable r) r.setRotation(f);
        put(x, y, z, d, text(a, b));
    }

    /** Yapının üstündeki ağaç ve arazi parçalarını temizler. */
    void clearance(int h) {
        Set<Long> cols = new HashSet<>();
        for (Op o : ops.values()) {
            if (!o.data().getMaterial().isAir()) cols.add(((long) (o.x() + 100000) << 20) | (long) (o.z() + 100000));
        }
        BlockData air = data(Material.AIR);
        for (long k : cols) {
            int x = (int) (k >> 20) - 100000;
            int z = (int) (k & 0xFFFFF) - 100000;
            for (int y = 1; y <= h; y++) {
                if (!ops.containsKey(key(x, y, z))) put(x, y, z, air, null);
            }
        }
    }

    void setSpawn(int x, int y, int z) {
        for (int i = 0; i < rot; i++) {
            int tmp = x;
            x = -z;
            z = tmp;
        }
        sx = x;
        sy = y;
        sz = z;
        spawnSet = true;
    }

    int spawnX() { return spawnSet ? sx : (minX + maxX) / 2; }

    int spawnZ() { return spawnSet ? sz : (minZ + maxZ) / 2; }

    int spawnY() {
        if (spawnSet) return sy;
        int x = (minX + maxX) / 2, z = (minZ + maxZ) / 2;
        for (int y = maxY; y >= minY; y--) {
            Op o = ops.get(key(x, y, z));
            if (o != null && !o.data().getMaterial().isAir()) return y + 1;
        }
        return 1;
    }

    int width() { return maxX < minX ? 1 : maxX - minX + 1; }

    int length() { return maxZ < minZ ? 1 : maxZ - minZ + 1; }

    List<Op> sorted() {
        List<Op> l = new ArrayList<>(ops.values());
        l.sort(Comparator.comparingInt(Op::y));
        return l;
    }
}
