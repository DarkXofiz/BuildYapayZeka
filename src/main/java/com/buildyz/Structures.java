package com.buildyz;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

/** Her çağrıda şekil, boyut, desen, yerleşim ve süslemeler rastgele seçilir. Hazır şablon yoktur. */
final class Structures {

    enum Kind {
        SPAWN("Spawn"), TRADE("Takas ve Kasa"), ARENA("PvP Arena"), BOXPVP("Box PvP"),
        PIT("FFA Çukuru"), DUELS("1v1 Kompleksi"), SKY("Gökyüzü PvP");

        final String label;

        Kind(String label) { this.label = label; }
    }

    private static final Material[] TEAM = {
            Material.RED_CONCRETE, Material.BLUE_CONCRETE, Material.LIME_CONCRETE, Material.YELLOW_CONCRETE,
            Material.ORANGE_CONCRETE, Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.WHITE_CONCRETE, Material.BLACK_CONCRETE};

    private Structures() {}

    /** sz: 0 küçük, 1 orta, 2 büyük, 3 dev. dim: hedef çap, 0 ise rastgele. */
    static Canvas build(Kind k, Theme t, int sz, int dim, long seed) {
        Random r = new Random(seed);
        Canvas c = new Canvas();
        c.rotate(r.nextInt(4));
        switch (k) {
            case SPAWN -> spawn(c, t, sz, dim, r);
            case TRADE -> trade(c, t, sz, dim, r);
            case ARENA -> arena(c, t, sz, dim, r);
            case BOXPVP -> boxpvp(c, t, sz, dim, r);
            case PIT -> pit(c, t, sz, dim, r);
            case DUELS -> duels(c, t, sz, dim, r);
            case SKY -> sky(c, t, sz, dim, r);
        }
        return c;
    }

    // ------------------------------------------------------------------ yardımcılar

    static BlockFace face(int dx, int dz) {
        if (dx > 0) return BlockFace.EAST;
        if (dx < 0) return BlockFace.WEST;
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static int rng(Random r, int lo, int hi) {
        return lo + r.nextInt(Math.max(1, hi - lo + 1));
    }

    private static int radius(Random r, int sz, int dim, int[] lo, int[] hi) {
        return dim > 0 ? Math.max(8, Math.min(100, dim / 2)) : rng(r, lo[sz], hi[sz]);
    }

    private static double dist(int shape, int x, int z) {
        double ax = Math.abs(x), az = Math.abs(z);
        return switch (shape) {
            case 1 -> Math.max(ax, az);
            case 2 -> Math.max(Math.max(ax, az), (ax + az) * 0.7071);
            default -> Math.sqrt(ax * ax + az * az);
        };
    }

    private static Material floorMat(Theme t, int pat, int x, int z, double d) {
        switch (pat) {
            case 1:
                return (((int) (d / 4)) & 1) == 0 ? t.base : t.alt;
            case 2: {
                int s = (int) Math.floor((Math.atan2(z, x) + Math.PI) * 4 / Math.PI);
                return (s & 1) == 0 ? t.base : t.alt;
            }
            case 3:
                return (Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0) ? t.alt : t.base;
            case 4:
                return Math.floorMod((x + z) / 3, 2) == 0 ? t.base : t.alt;
            default:
                return ((x + z) & 1) == 0 ? t.base : t.alt;
        }
    }

    private static void cyl(Canvas c, int cx, int cz, double rad, int y1, int y2, Material m) {
        int ri = (int) Math.ceil(rad);
        for (int dx = -ri; dx <= ri; dx++)
            for (int dz = -ri; dz <= ri; dz++)
                if (dx * dx + dz * dz <= rad * rad) c.box(cx + dx, y1, cz + dz, cx + dx, y2, cz + dz, m);
    }

    private static void post(Canvas c, Theme t, int x, int z, int h) {
        c.box(x, 1, z, x, h, z, t.pillar);
        c.set(x, h + 1, z, t.light);
    }

    private static Material[] teams(Random r) {
        int i = r.nextInt(TEAM.length);
        int j = (i + 1 + r.nextInt(TEAM.length - 1)) % TEAM.length;
        return new Material[]{TEAM[i], TEAM[j]};
    }

    private static void cover(Canvas c, Theme t, int type, int X, int Z, int sx, int sz, int h) {
        switch (type) {
            case 0 -> c.box(X, 1, Z, X + sx, h, Z + sz, t.wall);
            case 1 -> {
                c.box(X, 1, Z, X, h + 1, Z, t.pillar);
                c.set(X, h + 2, Z, t.light);
            }
            case 2 -> c.box(X - 1, 1, Z, X + 1, 2, Z, t.accent);
            case 3 -> {
                c.box(X - 1, 1, Z, X + 1, 1, Z, t.base);
                c.box(X, 1, Z - 1, X, 1, Z + 1, t.base);
                c.box(X, 2, Z, X, h, Z, t.pillar);
            }
            default -> c.box(X, 1, Z, X + sx, 1, Z + sz, t.alt);
        }
    }

    // ------------------------------------------------------------------ SPAWN

    private static void spawn(Canvas c, Theme t, int sz, int dim, Random r) {
        int R = radius(r, sz, dim, new int[]{20, 30, 46, 64}, new int[]{28, 42, 60, 90});
        int shape = r.nextInt(3), pat = r.nextInt(5);
        int pw = 2 + r.nextInt(3), wallH = 2 + r.nextInt(2);
        int sp = 7 + r.nextInt(4), postH = 4 + r.nextInt(4);
        for (int x = -R - 1; x <= R + 1; x++) {
            for (int z = -R - 1; z <= R + 1; z++) {
                double d = dist(shape, x, z);
                if (d > R) continue;
                boolean axis = Math.abs(x) <= pw || Math.abs(z) <= pw;
                c.box(x, -3, z, x, -1, z, t.alt);
                Material top = axis ? t.pillar : d > R - 2 ? t.accent : floorMat(t, pat, x, z, d);
                c.set(x, 0, z, top);
                if (d > R - 1 && !axis) c.box(x, 1, z, x, wallH, z, t.wall);
                else if (d > R - 1.5 && !axis && Math.floorMod(x * 7 + z * 11, sp) == 0) post(c, t, x, z, postH);
            }
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        String[] pool = {"PVP ARENA", "1V1 ODALARI", "TAKAS", "KASA", "SKY PVP", "BOX PVP", "FFA ÇUKURU", "MARKET"};
        int gh = 8 + r.nextInt(5), off = pw + 2;
        for (int[] dd : dirs) {
            int dx = dd[0], dz = dd[1];
            int gx = dx * (R - 1), gz = dz * (R - 1);
            int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
            for (int s = -1; s <= 1; s += 2)
                c.box(gx + lx * off * s, 1, gz + lz * off * s, gx + lx * off * s, gh, gz + lz * off * s, t.pillar);
            c.box(gx - lx * off, gh, gz - lz * off, gx + lx * off, gh, gz + lz * off, t.base);
            c.set(gx + lx * (off - 1), gh - 1, gz + lz * (off - 1), t.light);
            c.set(gx - lx * (off - 1), gh - 1, gz - lz * (off - 1), t.light);
            c.wallSign(gx - dx, gh, gz - dz, face(-dx, -dz), pool[r.nextInt(pool.length)], "->");
        }
        for (int k = 12; k < R - 4; k += 8 + r.nextInt(4)) {
            for (int s = -1; s <= 1; s += 2) {
                post(c, t, s * k, pw + 1, 4);
                post(c, t, -(pw + 1), s * k, 4);
            }
        }
        monument(c, t, r.nextInt(4), r);
        double rr = R * 0.62;
        int maxN = (int) (rr * Math.toRadians(54) / 6.5) + 1;
        int ns = Math.max(2, Math.min(rng(r, 3, 3 + R / 8), maxN));
        for (int i = 0; i < ns; i++) {
            double a = Math.toRadians(-72 + 54.0 * i / (ns - 1));
            int px = (int) Math.round(rr * Math.cos(a)), pz = (int) Math.round(rr * Math.sin(a));
            int dx = Math.abs(px) >= Math.abs(pz) ? -Integer.signum(px) : 0;
            int dz = dx == 0 ? -Integer.signum(pz) : 0;
            stall(c, t, px, pz, dx, dz, i + 1);
        }
        int vx = pw + 3, vz = pw + 3;
        int vw = Math.min(rng(r, 13, 25), R - vx - 4);
        int vl = Math.min(rng(r, 9, 15), (int) Math.sqrt(Math.max(0, (R - 3.0) * (R - 3.0) - (double) (vx + vw) * (vx + vw))) - vz);
        if (vw >= 9 && vl >= 7) vault(c, t, vx, vz, vw, vl, 'W');
        pad(c, t, -(int) (R * 0.55), -(int) (R * 0.45), "PVP", "ARENA");
        pad(c, t, -(int) (R * 0.55), (int) (R * 0.45), "SKY", "PVP");
        c.setSpawn(0, 1, 12);
        c.clearance(18);
    }

    private static void monument(Canvas c, Theme t, int type, Random r) {
        switch (type) {
            case 0 -> {
                int rb = 4 + r.nextInt(3);
                for (int x = -rb - 3; x <= rb + 3; x++) {
                    for (int z = -rb - 3; z <= rb + 3; z++) {
                        double d = Math.sqrt(x * x + z * z);
                        if (d <= 1.5) {
                            c.box(x, 0, z, x, 8, z, t.pillar);
                            c.set(x, 9, z, t.light);
                        } else if (d <= rb - 1.2) {
                            c.set(x, 0, z, Material.WATER);
                        } else if (d <= rb) {
                            c.set(x, 0, z, t.accent);
                            c.set(x, 1, z, t.base);
                        } else if (d <= rb + 2) {
                            c.set(x, 0, z, t.accent);
                        }
                    }
                }
            }
            case 1 -> {
                int H = 12 + r.nextInt(8);
                c.disc(0, 0, 0, 6, t.accent);
                for (int y = 1; y <= H; y++) {
                    int hw = y < H / 3 ? 2 : y < 2 * H / 3 ? 1 : 0;
                    c.box(-hw, y, -hw, hw, y, hw, t.pillar);
                }
                c.set(0, H + 1, 0, t.light);
            }
            case 2 -> {
                for (int k = 0; k < 6; k++) {
                    int s = 7 - k;
                    c.box(-s, 1, -s, s, k + 1, s, k % 2 == 0 ? t.base : t.alt);
                }
                c.set(0, 7, 0, t.light);
            }
            default -> {
                int n = 6 + r.nextInt(5);
                c.disc(0, 0, 0, 7, t.accent);
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n;
                    int px = (int) Math.round(6 * Math.cos(a)), pz = (int) Math.round(6 * Math.sin(a));
                    c.box(px, 1, pz, px, 8, pz, t.pillar);
                    c.set(px, 9, pz, t.light);
                }
                c.box(0, 1, 0, 0, 7, 0, t.glass);
                c.set(0, 8, 0, t.light);
            }
        }
    }

    private static void pad(Canvas c, Theme t, int cx, int cz, String a, String b) {
        c.disc(cx, 0, cz, 5, t.accent);
        c.disc(cx, 0, cz, 3, t.alt);
        post(c, t, cx - 4, cz, 4);
        post(c, t, cx + 4, cz, 4);
        c.standSign(cx, 1, cz, BlockFace.EAST, a, b);
    }

    private static void stall(Canvas c, Theme t, int cx, int cz, int dx, int dz, int id) {
        c.box(cx - 2, 0, cz - 2, cx + 2, 0, cz + 2, t.accent);
        for (int a = -2; a <= 2; a += 4)
            for (int b = -2; b <= 2; b += 4)
                c.box(cx + a, 1, cz + b, cx + a, 3, cz + b, Material.OAK_FENCE);
        c.box(cx - 2, 4, cz - 2, cx + 2, 4, cz + 2, t.alt);
        c.set(cx, 4, cz, t.light);
        int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
        for (int s = -1; s <= 1; s++) c.set(cx + dx * 2 + lx * s, 1, cz + dz * 2 + lz * s, Material.BARREL);
        c.standSign(cx + dx * 2, 2, cz + dz * 2, face(dx, dz), "TAKAS", "#" + id);
    }

    private static void booth(Canvas c, Theme t, int bx, int bz, int id) {
        c.box(bx - 4, 0, bz - 1, bx - 2, 0, bz + 1, t.accent);
        c.box(bx + 2, 0, bz - 1, bx + 4, 0, bz + 1, t.accent);
        c.box(bx - 1, 0, bz - 1, bx + 1, 0, bz + 1, t.alt);
        c.set(bx, 1, bz, Material.BARREL);
        c.standSign(bx, 2, bz, BlockFace.SOUTH, "TAKAS", "#" + id);
        c.box(bx, 1, bz - 1, bx, 2, bz - 1, t.glass);
        c.box(bx, 1, bz + 1, bx, 2, bz + 1, t.glass);
        post(c, t, bx - 4, bz - 1, 3);
        post(c, t, bx + 4, bz - 1, 3);
    }

    private static void vault(Canvas c, Theme t, int x0, int z0, int w, int l, char door) {
        int x1 = x0 + w - 1, z1 = z0 + l - 1, xc = (x0 + x1) / 2, zc = (z0 + z1) / 2;
        c.box(x0, -3, z0, x1, -1, z1, t.alt);
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                c.set(x, 0, z, ((x + z) & 1) == 0 ? t.accent : t.alt);
        for (int y = 1; y <= 5; y++) {
            c.box(x0, y, z0, x1, y, z0, t.wall);
            c.box(x0, y, z1, x1, y, z1, t.wall);
            c.box(x0, y, z0, x0, y, z1, t.wall);
            c.box(x1, y, z0, x1, y, z1, t.wall);
        }
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1})
                c.box(x, 1, z, x, 6, z, t.pillar);
        c.box(x0, 6, z0, x1, 6, z1, t.alt);
        for (int x = x0 + 3; x <= x1 - 3; x += 4) c.set(x, 6, zc, t.light);
        if (door == 'S') {
            c.air(xc - 1, 1, z1, xc + 1, 3, z1);
            c.wallSign(xc, 4, z1 + 1, BlockFace.SOUTH, "KASA", "ODASI");
        } else {
            c.air(x0, 1, zc - 1, x0, 3, zc + 1);
            c.wallSign(x0 - 1, 4, zc, BlockFace.WEST, "KASA", "ODASI");
        }
        int n = 1;
        for (int x = x0 + 2; x <= x1 - 2; x += 2) {
            if (door == 'S' && Math.abs(x - xc) <= 1) continue;
            c.facing(x, 1, z0 + 1, Material.CHEST, BlockFace.SOUTH);
            c.wallSign(x, 2, z0 + 1, BlockFace.SOUTH, "KASA", String.valueOf(n++));
            c.facing(x, 1, z1 - 1, Material.CHEST, BlockFace.NORTH);
            c.wallSign(x, 2, z1 - 1, BlockFace.NORTH, "KASA", String.valueOf(n++));
        }
        for (int x = x0 + 4; x <= x1 - 3; x += 2) c.facing(x, 1, zc, Material.CHEST, BlockFace.SOUTH);
    }

    // ------------------------------------------------------------------ ARENA

    private static void arena(Canvas c, Theme t, int sz, int dim, Random r) {
        int R = radius(r, sz, dim, new int[]{12, 18, 26, 36}, new int[]{17, 24, 34, 50});
        int shape = r.nextInt(3), pat = r.nextInt(5);
        int tiers = 3 + r.nextInt(5);
        int wallH = tiers + 2 + r.nextInt(5);
        int E = R + tiers + 4;
        for (int x = -E; x <= E; x++) {
            for (int z = -E; z <= E; z++) {
                double d = dist(shape, x, z);
                if (d <= R) {
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.set(x, 0, z, d <= 3 ? t.accent : floorMat(t, pat, x, z, d));
                } else if (d <= R + tiers) {
                    int h = (int) Math.ceil(d - R);
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.box(x, 0, z, x, h - 1, z, t.wall);
                    c.set(x, h, z, h % 2 == 0 ? t.alt : t.base);
                } else if (d <= R + tiers + 1.3) {
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.box(x, 0, z, x, wallH, z, t.wall);
                    if (((x + z) & 1) == 0) c.set(x, wallH + 1, z, t.base);
                    if (Math.floorMod(x * 3 + z * 5, 9) == 0) {
                        c.box(x, wallH + 1, z, x, wallH + 3, z, t.pillar);
                        c.set(x, wallH + 4, z, t.light);
                    }
                }
            }
        }
        // giriş tünelleri: 2 ile 4 arası, rastgele yönler
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int tun = 2 + r.nextInt(3), hw = 1 + r.nextInt(2);
        int start = r.nextInt(4);
        for (int i = 0; i < tun; i++) {
            int dx = dirs[(start + i) % 4][0], dz = dirs[(start + i) % 4][1];
            for (int a = R - 1; a <= R + tiers + 2; a++)
                for (int s = -hw; s <= hw; s++)
                    c.air(dx != 0 ? dx * a : s, 1, dz != 0 ? dz * a : s, dx != 0 ? dx * a : s, 4, dz != 0 ? dz * a : s);
        }
        // merkez
        int mid = r.nextInt(4);
        if (mid == 1) {
            int cr = 3 + r.nextInt(3);
            c.disc(0, 0, 0, cr, t.accent);
            c.disc(0, 1, 0, cr - 1, t.accent);
        } else if (mid == 2) {
            c.box(0, 1, 0, 0, 5 + r.nextInt(3), 0, t.pillar);
        } else if (mid == 3 && R >= 14) {
            for (int x = -8; x <= 8; x++)
                for (int z = -8; z <= 8; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d >= 5 && d <= 7 && Math.abs(x) > 1 && Math.abs(z) > 1) c.set(x, 0, z, Material.WATER);
                }
        }
        // simetrik siperler
        int n = 3 + R / 6 + r.nextInt(3);
        boolean fourFold = r.nextInt(10) < 7;
        int attempts = 0;
        for (int i = 0; i < n && attempts < 300; attempts++) {
            int px = 3 + r.nextInt(Math.max(1, R - 7)), pz = 3 + r.nextInt(Math.max(1, R - 7));
            if (dist(shape, px, pz) > R - 4 || (mid == 3 && Math.hypot(px, pz) < 9)) continue;
            int type = r.nextInt(5), hgt = 2 + r.nextInt(3);
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz2 = -1; sz2 <= 1; sz2 += 2) {
                    if (!fourFold && sz2 < 0) continue;
                    cover(c, t, type, sx * px, sz2 * pz, sx, sz2, hgt);
                }
            i++;
        }
        Material[] tm = teams(r);
        c.disc(R - 3, 0, 0, 2, tm[0]);
        c.disc(-(R - 3), 0, 0, 2, tm[1]);
        c.setSpawn(0, 1, 0);
        c.clearance(wallH + 6);
    }

    // ------------------------------------------------------------------ BOX PVP

    private static void boxpvp(Canvas c, Theme t, int sz, int dim, Random r) {
        int W = dim > 0 ? Math.max(16, Math.min(120, dim))
                : rng(r, new int[]{20, 28, 40, 56}[sz], new int[]{28, 40, 56, 80}[sz]);
        int L = r.nextBoolean() ? W : Math.max(16, (int) (W * (0.6 + r.nextDouble() * 0.4)));
        int H = rng(r, 10, 14 + sz * 4);
        int pat = r.nextInt(5);
        boolean roof = r.nextBoolean();
        c.box(0, -3, 0, W - 1, -1, L - 1, t.alt);
        for (int x = 0; x < W; x++)
            for (int z = 0; z < L; z++)
                c.set(x, 0, z, floorMat(t, pat, x, z, Math.hypot(x - W / 2.0, z - L / 2.0)));
        c.box(0, 1, 0, W - 1, H, 0, t.glass);
        c.box(0, 1, L - 1, W - 1, H, L - 1, t.glass);
        c.box(0, 1, 0, 0, H, L - 1, t.glass);
        c.box(W - 1, 1, 0, W - 1, H, L - 1, t.glass);
        if (roof) c.box(0, H + 1, 0, W - 1, H + 1, L - 1, t.glass);
        c.box(0, H, 0, W - 1, H, 0, t.pillar);
        c.box(0, H, L - 1, W - 1, H, L - 1, t.pillar);
        c.box(0, H, 0, 0, H, L - 1, t.pillar);
        c.box(W - 1, H, 0, W - 1, H, L - 1, t.pillar);
        for (int x = 0; x < W; x += 8) {
            c.box(x, 1, 0, x, H, 0, t.pillar);
            c.box(x, 1, L - 1, x, H, L - 1, t.pillar);
            c.set(x, H + 1, 0, t.light);
            c.set(x, H + 1, L - 1, t.light);
        }
        for (int z = 0; z < L; z += 8) {
            c.box(0, 1, z, 0, H, z, t.pillar);
            c.box(W - 1, 1, z, W - 1, H, z, t.pillar);
        }
        c.box(W - 1, 1, L - 1, W - 1, H, L - 1, t.pillar);
        // iç platformlar (x ekseninde simetrik)
        int np = 3 + sz * 2 + r.nextInt(3);
        for (int i = 0; i < np; i++) {
            int pw = rng(r, 3, 7), pl = rng(r, 3, 7);
            int x = rng(r, 3, Math.max(3, W / 2 - pw)), z = rng(r, 3, Math.max(3, L - pl - 3));
            int y = rng(r, 3, Math.max(3, H - 3));
            Material m = r.nextBoolean() ? t.alt : r.nextBoolean() ? t.accent : t.base;
            c.box(x, y, z, x + pw - 1, y, z + pl - 1, m);
            c.box(W - 1 - x, y, z, W - 1 - (x + pw - 1), y, z + pl - 1, m);
            if (r.nextBoolean()) {
                c.box(x + pw / 2, 1, z + pl / 2, x + pw / 2, y - 1, z + pl / 2, t.pillar);
                c.box(W - 1 - (x + pw / 2), 1, z + pl / 2, W - 1 - (x + pw / 2), y - 1, z + pl / 2, t.pillar);
            }
        }
        // zemin siperleri
        int nc = 2 + sz + r.nextInt(3);
        for (int i = 0; i < nc; i++) {
            int x = rng(r, 6, Math.max(6, W / 2 - 2)), z = rng(r, 3, L - 4);
            int h = 2 + r.nextInt(4);
            c.box(x, 1, z, x + 1, h, z + 1, t.wall);
            c.box(W - 1 - x, 1, z, W - 2 - x, h, z + 1, t.wall);
        }
        Material[] tm = teams(r);
        c.box(2, 0, L / 2 - 1, 4, 0, L / 2 + 1, tm[0]);
        c.box(W - 5, 0, L / 2 - 1, W - 3, 0, L / 2 + 1, tm[1]);
        c.air(W / 2 - 1, 1, L - 1, W / 2 + 1, 3, L - 1);
        c.wallSign(W / 2, 5, L, BlockFace.SOUTH, "BOX PVP", "GİRİŞ");
        c.setSpawn(W / 2, 1, L - 3);
        c.clearance(H + 4);
    }

    // ------------------------------------------------------------------ FFA ÇUKURU

    private static void pit(Canvas c, Theme t, int sz, int dim, Random r) {
        int R = radius(r, sz, dim, new int[]{10, 14, 20, 28}, new int[]{14, 20, 28, 40});
        int depth = rng(r, 8, 13 + sz * 2), shape = r.nextInt(3), pat = r.nextInt(5);
        for (int x = -R - 6; x <= R + 6; x++) {
            for (int z = -R - 6; z <= R + 6; z++) {
                double d = dist(shape, x, z);
                if (d <= R) {
                    c.air(x, -depth + 1, z, x, 0, z);
                    c.box(x, -depth - 3, z, x, -depth - 1, z, t.alt);
                    c.set(x, -depth, z, floorMat(t, pat, x, z, d));
                } else if (d <= R + 1.5) {
                    c.box(x, -depth - 3, z, x, 0, z, t.wall);
                    if (Math.floorMod(x * 5 + z * 7, 5) == 0) c.set(x, -depth / 2, z, t.light);
                } else if (d <= R + 3.5) {
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.set(x, 0, z, t.accent);
                    if (d > R + 2.5) {
                        c.set(x, 1, z, t.base);
                        if (Math.floorMod(x * 3 + z * 5, 8) == 0) post(c, t, x, z, 4);
                    }
                }
            }
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int rampCount = 1 + r.nextInt(3), start = r.nextInt(4);
        for (int k = 0; k < rampCount; k++) {
            int dx = dirs[(start + k) % 4][0], dz = dirs[(start + k) % 4][1];
            for (int i = 0; i < depth; i++) {
                int a = R - i;
                for (int s = -1; s <= 1; s++) {
                    int x = dx != 0 ? dx * a : s, z = dz != 0 ? dz * a : s;
                    c.box(x, -depth + 1, z, x, -i, z, t.base);
                }
            }
        }
        int np = 3 + sz * 2 + r.nextInt(3);
        for (int i = 0; i < np; i++) {
            int px = r.nextInt(Math.max(1, R - 4)), pz = r.nextInt(Math.max(1, R - 4));
            if (dist(shape, px, pz) > R - 3) continue;
            int h = 2 + r.nextInt(Math.max(1, depth - 3));
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz2 = -1; sz2 <= 1; sz2 += 2) {
                    c.box(sx * px, -depth + 1, sz2 * pz, sx * px, -depth + h, sz2 * pz, t.pillar);
                    c.set(sx * px, -depth + h + 1, sz2 * pz, t.accent);
                }
        }
        int pools = r.nextInt(3);
        for (int i = 0; i < pools; i++) {
            int px = r.nextInt(Math.max(1, R - 6)) - R / 3, pz = r.nextInt(Math.max(1, R - 6)) - R / 3;
            if (Math.hypot(px, pz) < R - 5) c.disc(px, -depth, pz, 2 + r.nextInt(2), Material.WATER);
        }
        c.setSpawn(0, 1, R + 2);
        c.clearance(10);
    }

    // ------------------------------------------------------------------ 1v1

    private static void duels(Canvas c, Theme t, int sz, int dim, Random r) {
        int w = 11 + 2 * r.nextInt(5), g = 4 + r.nextInt(3), pitch = w + g;
        int cols, rows;
        if (dim > 0) {
            cols = Math.max(2, Math.min(8, dim / pitch));
            rows = cols;
        } else {
            cols = rng(r, 2, 3 + sz);
            rows = rng(r, 2, 3 + sz);
        }
        boolean roof = r.nextBoolean();
        int H = 5 + r.nextInt(3), pat = r.nextInt(5);
        int spanX = cols * pitch + g, spanZ = rows * pitch + g;
        c.box(0, -3, 0, spanX - 1, -1, spanZ - 1, t.alt);
        c.box(0, 0, 0, spanX - 1, 0, spanZ - 1, t.pillar);
        c.box(0, 1, 0, spanX - 1, 2, 0, t.wall);
        c.box(0, 1, spanZ - 1, spanX - 1, 2, spanZ - 1, t.wall);
        c.box(0, 1, 0, 0, 2, spanZ - 1, t.wall);
        c.box(spanX - 1, 1, 0, spanX - 1, 2, spanZ - 1, t.wall);
        int id = 1;
        for (int j = 0; j < rows; j++)
            for (int i = 0; i < cols; i++)
                cell(c, t, g + i * pitch, g + j * pitch, w, H, id++, r.nextInt(5), pat, roof, r);
        for (int i = 0; i <= cols; i++)
            for (int j = 0; j <= rows; j++)
                post(c, t, Math.min(i * pitch + g / 2, spanX - 3), Math.min(j * pitch + g / 2, spanZ - 3), 4);
        c.setSpawn(g / 2, 1, g / 2);
        c.clearance(H + 5);
    }

    private static void cell(Canvas c, Theme t, int x0, int z0, int w, int H, int id, int style, int pat,
                             boolean roof, Random r) {
        int x1 = x0 + w - 1, z1 = z0 + w - 1, xc = (x0 + x1) / 2, zc = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                c.set(x, 0, z, floorMat(t, pat, x, z, Math.hypot(x - xc, z - zc)));
        for (int y = 1; y <= H; y++) {
            Material m = (y == 3 || y == 4) ? t.glass : t.wall;
            c.box(x0, y, z0, x1, y, z0, m);
            c.box(x0, y, z1, x1, y, z1, m);
            c.box(x0, y, z0, x0, y, z1, m);
            c.box(x1, y, z0, x1, y, z1, m);
        }
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1})
                c.box(x, 1, z, x, H + 1, z, t.pillar);
        if (roof) {
            c.box(x0, H + 1, z0, x1, H + 1, z1, t.glass);
        } else {
            c.box(x0, H + 1, z0, x1, H + 1, z0, t.base);
            c.box(x0, H + 1, z1, x1, H + 1, z1, t.base);
            c.box(x0, H + 1, z0, x0, H + 1, z1, t.base);
            c.box(x1, H + 1, z0, x1, H + 1, z1, t.base);
        }
        c.set(x0 + 2, H - 1, z0 + 2, t.light);
        c.set(x1 - 2, H - 1, z0 + 2, t.light);
        c.set(x0 + 2, H - 1, z1 - 2, t.light);
        c.set(x1 - 2, H - 1, z1 - 2, t.light);
        c.air(xc - 1, 1, z1, xc + 1, 3, z1);
        c.wallSign(xc, 5, z1 + 1, BlockFace.SOUTH, "1V1 #" + id, "HAZIR");
        Material[] tm = teams(r);
        c.box(x0 + 2, 0, zc - 1, x0 + 4, 0, zc + 1, tm[0]);
        c.box(x1 - 4, 0, zc - 1, x1 - 2, 0, zc + 1, tm[1]);
        switch (style) {
            case 1 -> c.box(xc, 1, zc, xc, 3, zc, t.pillar);
            case 2 -> {
                int ox = 2 + r.nextInt(2), oz = 2 + r.nextInt(Math.max(1, w / 2 - 3));
                for (int sx = -1; sx <= 1; sx += 2)
                    for (int sz = -1; sz <= 1; sz += 2)
                        c.box(xc + sx * ox, 1, zc + sz * oz, xc + sx * ox, 2, zc + sz * oz, t.accent);
            }
            case 3 -> {
                c.box(xc - 2, 1, zc, xc + 2, 2, zc, t.accent);
                c.box(xc, 1, zc - 2, xc, 2, zc + 2, t.accent);
                c.air(xc, 1, zc, xc, 2, zc);
            }
            case 4 -> {
                c.box(xc - 2, 1, zc - 2, xc + 2, 1, zc + 2, t.alt);
                c.box(xc - 2, 2, zc - 2, xc - 2, 3, zc - 2, t.pillar);
                c.box(xc + 2, 2, zc + 2, xc + 2, 3, zc + 2, t.pillar);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ SKY

    private static void sky(Canvas c, Theme t, int sz, int dim, Random r) {
        int Rc = radius(r, sz, dim, new int[]{12, 16, 22, 30}, new int[]{16, 22, 30, 42});
        int Y = 24 + r.nextInt(30);
        island(c, t, 0, 0, Rc, Y, r);
        decor(c, t, 0, 0, Rc, Y, r);
        int n = 3 + r.nextInt(4) + sz;
        double off = r.nextDouble() * Math.PI * 2;
        double base = Rc * (1.9 + r.nextDouble() * 0.8) + 6;
        int[] px = new int[n], pz = new int[n], pr = new int[n], py = new int[n];
        for (int i = 0; i < n; i++) {
            double a = off + 2 * Math.PI * i / n + (r.nextDouble() - 0.5) * 0.3;
            double dd = base * (0.85 + r.nextDouble() * 0.4);
            px[i] = (int) Math.round(Math.cos(a) * dd);
            pz[i] = (int) Math.round(Math.sin(a) * dd);
            pr[i] = Math.max(5, (int) (Rc * (0.35 + r.nextDouble() * 0.35)));
            py[i] = Y + r.nextInt(7) - 3;
            island(c, t, px[i], pz[i], pr[i], py[i], r);
            decor(c, t, px[i], pz[i], pr[i], py[i], r);
            link(c, t, 0, 0, Rc, Y, px[i], pz[i], pr[i], py[i]);
        }
        if (r.nextBoolean()) {
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                link(c, t, px[i], pz[i], pr[i], py[i], px[j], pz[j], pr[j], py[j]);
            }
        }
        c.setSpawn(0, Y + 1, 0);
    }

    private static void decor(Canvas c, Theme t, int cx, int cz, int rad, int Y, Random r) {
        if (rad < 8) {
            c.box(cx, Y + 1, cz, cx, Y + 3, cz, t.pillar);
            c.set(cx, Y + 4, cz, t.light);
            return;
        }
        int h = rad / 2, ph = 4 + r.nextInt(3);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                c.box(cx + sx * h, Y + 1, cz + sz * h, cx + sx * h, Y + ph, cz + sz * h, t.pillar);
                c.set(cx + sx * h, Y + ph + 1, cz + sz * h, t.light);
            }
        }
        int q = rad / 3;
        c.box(cx + q - 1, Y + 1, cz, cx + q + 1, Y + 2, cz, t.accent);
        c.box(cx - q - 1, Y + 1, cz, cx - q + 1, Y + 2, cz, t.accent);
        if (r.nextBoolean()) {
            c.box(cx, Y + 1, cz + q - 1, cx, Y + 2, cz + q + 1, t.accent);
            c.box(cx, Y + 1, cz - q - 1, cx, Y + 2, cz - q + 1, t.accent);
        }
    }

    private static void island(Canvas c, Theme t, int cx, int cz, int rad, int top, Random r) {
        int depth = (int) (rad * (0.7 + r.nextDouble() * 0.5));
        int pat = r.nextInt(5);
        for (int l = 0; l <= depth; l++) {
            double rl = rad * (1.0 - l / (depth + 1.0));
            int ri = (int) Math.ceil(rl);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > rl) continue;
                    Material m;
                    if (l == 0) m = d > rad - 1.5 ? t.accent : floorMat(t, pat, dx, dz, d);
                    else if (l < 3) m = t.wall;
                    else m = r.nextInt(4) == 0 ? t.pillar : t.alt;
                    c.set(cx + dx, top - l, cz + dz, m);
                }
            }
        }
    }

    private static void link(Canvas c, Theme t, int x1, int z1, int r1, int y1, int x2, int z2, int r2, int y2) {
        double dx = x2 - x1, dz = z2 - z1, len = Math.hypot(dx, dz);
        if (len < 1) return;
        double ux = dx / len, uz = dz / len;
        double sx = x1 + ux * (r1 - 1), sz = z1 + uz * (r1 - 1);
        double ex = x2 - ux * (r2 - 1), ez = z2 - uz * (r2 - 1);
        int n = (int) Math.hypot(ex - sx, ez - sz);
        for (int i = 0; i <= n; i++) {
            double f = i / (double) Math.max(1, n);
            int x = (int) Math.round(sx + (ex - sx) * f), z = (int) Math.round(sz + (ez - sz) * f);
            int y = (int) Math.round(y1 + (y2 - y1) * f);
            c.disc(x, y, z, 1.5, t.pillar);
            if (i % 8 == 4) {
                c.set(x, y + 1, z, t.light);
            }
        }
    }

    // ------------------------------------------------------------------ TAKAS VE KASA

    private static void trade(Canvas c, Theme t, int sz, int dim, Random r) {
        int W = dim > 0 ? Math.max(36, Math.min(140, dim))
                : rng(r, new int[]{40, 54, 70, 92}[sz], new int[]{52, 68, 92, 130}[sz]);
        int L = Math.max(30, (int) (W * (0.6 + r.nextDouble() * 0.25)));
        int pat = r.nextInt(5), lane = 1 + r.nextInt(3);
        c.box(0, -3, 0, W - 1, -1, L - 1, t.alt);
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                boolean ln = Math.abs(x - W / 2) <= lane + 1 || Math.abs(z - L / 2) <= lane;
                c.set(x, 0, z, ln ? t.pillar : floorMat(t, pat, x, z, Math.hypot(x - W / 2.0, z - L / 2.0)));
            }
        }
        for (int x = 0; x < W; x++) {
            c.box(x, 1, 0, x, 2, 0, t.wall);
            c.box(x, 1, L - 1, x, 2, L - 1, t.wall);
        }
        for (int z = 0; z < L; z++) {
            c.box(0, 1, z, 0, 2, z, t.wall);
            c.box(W - 1, 1, z, W - 1, 2, z, t.wall);
        }
        int ps = 7 + r.nextInt(4);
        for (int x = 0; x < W; x += ps) {
            post(c, t, x, 0, 5);
            post(c, t, x, L - 1, 5);
        }
        for (int z = 0; z < L; z += ps) {
            post(c, t, 0, z, 5);
            post(c, t, W - 1, z, 5);
        }
        c.air(0, 1, L / 2 - 2, 0, 2, L / 2 + 2);
        c.air(W - 1, 1, L / 2 - 2, W - 1, 2, L / 2 + 2);
        int vw = Math.min(W - 10, rng(r, 17, 40)), vl = rng(r, 9, 15);
        int vx0 = (W - vw) / 2, vz0 = -vl - 1;
        vault(c, t, vx0, vz0, vw, vl, 'S');
        int xc = vx0 + (vw - 1) / 2;
        c.air(xc - 2, 1, 0, xc + 2, 2, 0);
        c.box(xc - 2, -3, -1, xc + 2, -1, -1, t.alt);
        c.box(xc - 2, 0, -1, xc + 2, 0, -1, t.pillar);
        int id = 1, sp = 7 + r.nextInt(3);
        for (int x = 8; x <= W - 8; x += sp) {
            stall(c, t, x, 6, 0, 1, id++);
            stall(c, t, x, L - 7, 0, -1, id++);
        }
        int rowsN = 1 + r.nextInt(2);
        int per = rng(r, 2, 2 + W / 18);
        for (int row = 0; row < rowsN; row++) {
            int bz = rowsN == 1 ? L / 2 - 5 : L / 2 + (row == 0 ? -5 : 5);
            for (int k = 0; k < per; k++) {
                int bx = 10 + (int) ((W - 20.0) * k / (per - 1));
                booth(c, t, bx, bz, row * per + k + 1);
            }
        }
        c.setSpawn(W / 2, 1, L / 2);
        c.clearance(14);
    }
}
