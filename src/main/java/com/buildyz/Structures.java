package com.buildyz;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Random;

final class Structures {

    enum Kind {
        SPAWN("Spawn"), ARENA("PvP Arena"), DUELS("1v1 Kompleksi"), SKY("Gökyüzü PvP"), TRADE("Takas ve Kasa");

        final String label;

        Kind(String label) { this.label = label; }
    }

    private Structures() {}

    static Canvas build(Kind k, Theme t, int size, long seed) {
        Random r = new Random(seed);
        Canvas c = new Canvas();
        switch (k) {
            case SPAWN -> spawn(c, t, size, r);
            case ARENA -> arena(c, t, size, r);
            case DUELS -> duels(c, t, size, r);
            case SKY -> sky(c, t, size, r);
            case TRADE -> trade(c, t, size, r);
        }
        return c;
    }

    // ------------------------------------------------------------------ yardımcılar

    static BlockFace face(int dx, int dz) {
        if (dx > 0) return BlockFace.EAST;
        if (dx < 0) return BlockFace.WEST;
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static double dist(int shape, int x, int z) {
        double ax = Math.abs(x), az = Math.abs(z);
        return switch (shape) {
            case 1 -> Math.max(ax, az);
            case 2 -> Math.max(Math.max(ax, az), (ax + az) * 0.7071);
            default -> Math.sqrt(ax * ax + az * az);
        };
    }

    private static Material checker(Theme t, int x, int z) {
        return ((x + z) & 1) == 0 ? t.base : t.alt;
    }

    private static void post(Canvas c, Theme t, int x, int z, int h) {
        c.box(x, 1, z, x, h, z, t.pillar);
        c.set(x, h + 1, z, t.light);
    }

    // ------------------------------------------------------------------ SPAWN

    private static void spawn(Canvas c, Theme t, int size, Random r) {
        int R = new int[]{26, 38, 52}[size];
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > R) continue;
                boolean axis = Math.abs(x) <= 3 || Math.abs(z) <= 3;
                c.box(x, -3, z, x, -1, z, t.alt);
                Material top;
                if (axis) top = t.pillar;
                else if (d > R - 2) top = t.accent;
                else top = ((int) (d / 4)) % 2 == 0 ? t.base : t.alt;
                c.set(x, 0, z, top);
                if (d > R - 1 && !axis) c.box(x, 1, z, x, 2, z, t.wall);
            }
        }
        // çevre sütunları
        int n = (int) (2 * Math.PI * R / 9);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            int px = (int) Math.round((R - 0.5) * Math.cos(a));
            int pz = (int) Math.round((R - 0.5) * Math.sin(a));
            if (Math.abs(px) <= 5 || Math.abs(pz) <= 5) continue;
            post(c, t, px, pz, 5);
        }
        // dört kapı
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        String[] names = {"PVP ARENA", "1V1 ODALARI", "TAKAS VE KASA", "SKY PVP"};
        for (int i = 0; i < 4; i++) {
            int dx = dirs[i][0], dz = dirs[i][1];
            int gx = dx * (R - 1), gz = dz * (R - 1);
            int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
            for (int s = -1; s <= 1; s += 2) {
                c.box(gx + lx * 5 * s, 1, gz + lz * 5 * s, gx + lx * 5 * s, 9, gz + lz * 5 * s, t.pillar);
            }
            c.box(gx - lx * 5, 9, gz - lz * 5, gx + lx * 5, 9, gz + lz * 5, t.base);
            c.set(gx + lx * 3, 8, gz + lz * 3, t.light);
            c.set(gx - lx * 3, 8, gz - lz * 3, t.light);
            c.wallSign(gx - dx, 9, gz - dz, face(-dx, -dz), names[i], "->");
        }
        // sokak lambaları
        for (int k = 12; k < R - 4; k += 10) {
            for (int s = -1; s <= 1; s += 2) {
                post(c, t, s * k, 5, 4);
                post(c, t, -5, s * k, 4);
            }
        }
        // merkez çeşme
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 1.5) {
                    c.box(x, 0, z, x, 8, z, t.pillar);
                    c.set(x, 9, z, t.light);
                } else if (d <= 4.3) {
                    c.set(x, 0, z, Material.WATER);
                } else if (d <= 5.5) {
                    c.set(x, 0, z, t.accent);
                    c.set(x, 1, z, t.base);
                } else if (d <= 7.5) {
                    c.set(x, 0, z, t.accent);
                }
            }
        }
        // takas tezgahları (kuzeydoğu)
        int ns = 3 + size * 2;
        double rr = R * 0.62;
        for (int i = 0; i < ns; i++) {
            double a = Math.toRadians(-72 + 54.0 * i / (ns - 1));
            int px = (int) Math.round(rr * Math.cos(a));
            int pz = (int) Math.round(rr * Math.sin(a));
            int dx = Math.abs(px) >= Math.abs(pz) ? -Integer.signum(px) : 0;
            int dz = dx == 0 ? -Integer.signum(pz) : 0;
            stall(c, t, px, pz, dx, dz, i + 1);
        }
        // kasa odası (güneydoğu)
        vault(c, t, 7 + size * 3, 7, new int[]{13, 17, 23}[size], new int[]{9, 11, 13}[size], 'W');
        // pvp ve sky giriş platformları (batı)
        pad(c, t, -(int) (R * 0.55), -(int) (R * 0.45), "PVP", "ARENA");
        pad(c, t, -(int) (R * 0.55), (int) (R * 0.45), "SKY", "PVP");
        c.setSpawn(0, 1, 9);
        c.clearance(14);
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

    private static void arena(Canvas c, Theme t, int size, Random r) {
        int R = new int[]{16, 22, 30}[size];
        int shape = r.nextInt(3);
        int E = R + 10;
        for (int x = -E; x <= E; x++) {
            for (int z = -E; z <= E; z++) {
                double d = dist(shape, x, z);
                if (d <= R) {
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.set(x, 0, z, d <= 3 ? t.accent : checker(t, x, z));
                } else if (d <= R + 6) {
                    int h = (int) Math.ceil(d - R);
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.box(x, 0, z, x, h - 1, z, t.wall);
                    c.set(x, h, z, h % 2 == 0 ? t.alt : t.base);
                } else if (d <= R + 7.3) {
                    c.box(x, -3, z, x, -1, z, t.alt);
                    c.box(x, 0, z, x, 9, z, t.wall);
                    if (((x + z) & 1) == 0) c.set(x, 10, z, t.base);
                    if ((x * 3 + z * 5) % 9 == 0) {
                        c.box(x, 10, z, x, 12, z, t.pillar);
                        c.set(x, 13, z, t.light);
                    }
                }
            }
        }
        // dört giriş tüneli
        for (int x = -E; x <= E; x++) {
            for (int z = -E; z <= E; z++) {
                double d = dist(shape, x, z);
                if (d > R + 8) continue;
                boolean gx = Math.abs(x) <= 2 && Math.abs(z) >= R - 1;
                boolean gz = Math.abs(z) <= 2 && Math.abs(x) >= R - 1;
                if (gx || gz) c.air(x, 1, z, x, 4, z);
            }
        }
        // simetrik siper
        int n = 3 + size * 2;
        int attempts = 0;
        for (int i = 0; i < n && attempts < 200; attempts++) {
            int px = 3 + r.nextInt(Math.max(1, R - 8));
            int pz = 3 + r.nextInt(Math.max(1, R - 8));
            if (dist(shape, px, pz) > R - 4) continue;
            int type = r.nextInt(3);
            int hgt = 2 + r.nextInt(2);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    int X = sx * px, Z = sz * pz;
                    if (type == 0) {
                        c.box(X, 1, Z, X + sx, hgt, Z + sz, t.wall);
                    } else if (type == 1) {
                        c.box(X, 1, Z, X, hgt + 1, Z, t.pillar);
                        c.set(X, hgt + 2, Z, t.light);
                    } else {
                        c.box(X - 1, 1, Z, X + 1, 2, Z, t.accent);
                    }
                }
            }
            i++;
        }
        c.disc(R - 3, 0, 0, 2, Material.RED_CONCRETE);
        c.disc(-(R - 3), 0, 0, 2, Material.BLUE_CONCRETE);
        c.setSpawn(0, 1, 0);
        c.clearance(16);
    }

    // ------------------------------------------------------------------ 1v1

    private static void duels(Canvas c, Theme t, int size, Random r) {
        int w = new int[]{13, 15, 17}[size];
        int n = 2 + size, g = 5, pitch = w + g, span = n * pitch + g;
        c.box(0, -3, 0, span - 1, -1, span - 1, t.alt);
        c.box(0, 0, 0, span - 1, 0, span - 1, t.pillar);
        c.box(0, 1, 0, span - 1, 2, 0, t.wall);
        c.box(0, 1, span - 1, span - 1, 2, span - 1, t.wall);
        c.box(0, 1, 0, 0, 2, span - 1, t.wall);
        c.box(span - 1, 1, 0, span - 1, 2, span - 1, t.wall);
        int id = 1;
        for (int j = 0; j < n; j++)
            for (int i = 0; i < n; i++)
                cell(c, t, g + i * pitch, g + j * pitch, w, id++, r);
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++)
                post(c, t, Math.min(i * pitch + 2, span - 3), Math.min(j * pitch + 2, span - 3), 4);
        c.setSpawn(2, 1, 2);
        c.clearance(12);
    }

    private static void cell(Canvas c, Theme t, int x0, int z0, int w, int id, Random r) {
        int x1 = x0 + w - 1, z1 = z0 + w - 1, xc = (x0 + x1) / 2, zc = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                c.set(x, 0, z, checker(t, x, z));
        for (int y = 1; y <= 6; y++) {
            Material m = (y == 3 || y == 4) ? t.glass : t.wall;
            c.box(x0, y, z0, x1, y, z0, m);
            c.box(x0, y, z1, x1, y, z1, m);
            c.box(x0, y, z0, x0, y, z1, m);
            c.box(x1, y, z0, x1, y, z1, m);
        }
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1})
                c.box(x, 1, z, x, 7, z, t.pillar);
        c.box(x0, 7, z0, x1, 7, z1, t.glass);
        c.box(x0, 8, z0, x1, 8, z0, t.base);
        c.box(x0, 8, z1, x1, 8, z1, t.base);
        c.box(x0, 8, z0, x0, 8, z1, t.base);
        c.box(x1, 8, z0, x1, 8, z1, t.base);
        c.set(x0 + 2, 6, z0 + 2, t.light);
        c.set(x1 - 2, 6, z0 + 2, t.light);
        c.set(x0 + 2, 6, z1 - 2, t.light);
        c.set(x1 - 2, 6, z1 - 2, t.light);
        c.air(xc - 1, 1, z1, xc + 1, 3, z1);
        c.wallSign(xc, 5, z1 + 1, BlockFace.SOUTH, "1V1 #" + id, "HAZIR");
        c.box(x0 + 2, 0, zc - 1, x0 + 4, 0, zc + 1, Material.RED_CONCRETE);
        c.box(x1 - 4, 0, zc - 1, x1 - 2, 0, zc + 1, Material.BLUE_CONCRETE);
        c.box(xc, 1, zc, xc, 3, zc, t.pillar);
        int ox = 2 + r.nextInt(2), oz = 2 + r.nextInt(Math.max(1, w / 2 - 3));
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
                c.box(xc + sx * ox, 1, zc + sz * oz, xc + sx * ox, 2, zc + sz * oz, t.accent);
    }

    // ------------------------------------------------------------------ SKY

    private static void sky(Canvas c, Theme t, int size, Random r) {
        int R = new int[]{14, 18, 24}[size];
        int Y = 30, D = R * 2 + 6, S = R / 2 + 2;
        island(c, t, 0, 0, R, Y, r);
        int h = R / 2;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                c.box(sx * h, Y + 1, sz * h, sx * h, Y + 5, sz * h, t.pillar);
                c.set(sx * h, Y + 6, sz * h, t.light);
            }
        }
        int q = R / 3;
        c.box(q - 1, Y + 1, 0, q + 1, Y + 2, 0, t.accent);
        c.box(-q - 1, Y + 1, 0, -q + 1, Y + 2, 0, t.accent);
        c.box(0, Y + 1, q - 1, 0, Y + 2, q + 1, t.accent);
        c.box(0, Y + 1, -q - 1, 0, Y + 2, -q + 1, t.accent);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int cx = d[0] * D, cz = d[1] * D;
            island(c, t, cx, cz, S, Y, r);
            for (int a = R; a <= D - S; a++) {
                for (int s = -1; s <= 1; s++) {
                    int x = d[0] != 0 ? d[0] * a : s;
                    int z = d[1] != 0 ? d[1] * a : s;
                    c.set(x, Y, z, t.pillar);
                    if (s != 0 && a % 2 == 0) c.set(x, Y + 1, z, t.glass);
                }
            }
            c.box(cx, Y + 1, cz, cx, Y + 4, cz, t.pillar);
            c.set(cx, Y + 5, cz, t.light);
        }
        c.setSpawn(0, Y + 1, 0);
    }

    private static void island(Canvas c, Theme t, int cx, int cz, int rad, int top, Random r) {
        int depth = (int) (rad * 0.9);
        for (int l = 0; l <= depth; l++) {
            double rl = rad * (1.0 - l / (depth + 1.0));
            int ri = (int) Math.ceil(rl);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > rl) continue;
                    Material m;
                    if (l == 0) m = d > rad - 1.5 ? t.accent : checker(t, dx, dz);
                    else if (l < 3) m = t.wall;
                    else m = r.nextInt(4) == 0 ? t.pillar : t.alt;
                    c.set(cx + dx, top - l, cz + dz, m);
                }
            }
        }
    }

    // ------------------------------------------------------------------ TAKAS VE KASA

    private static void trade(Canvas c, Theme t, int size, Random r) {
        int W = new int[]{44, 60, 76}[size], L = new int[]{34, 44, 54}[size];
        c.box(0, -3, 0, W - 1, -1, L - 1, t.alt);
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                boolean lane = Math.abs(x - W / 2) <= 2 || Math.abs(z - L / 2) <= 1;
                c.set(x, 0, z, lane ? t.pillar : ((((x >> 2) + (z >> 2)) & 1) == 0 ? t.base : t.alt));
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
        for (int x = 0; x < W; x += 8) {
            post(c, t, x, 0, 5);
            post(c, t, x, L - 1, 5);
        }
        for (int z = 0; z < L; z += 8) {
            post(c, t, 0, z, 5);
            post(c, t, W - 1, z, 5);
        }
        c.air(0, 1, L / 2 - 2, 0, 2, L / 2 + 2);
        c.air(W - 1, 1, L / 2 - 2, W - 1, 2, L / 2 + 2);
        // kasa binası (kuzey)
        int vw = W - 10, vl = 9 + size * 2, vx0 = 5, vz0 = -vl - 1;
        vault(c, t, vx0, vz0, vw, vl, 'S');
        int xc = (vx0 + vx0 + vw - 1) / 2;
        c.air(xc - 2, 1, 0, xc + 2, 2, 0);
        c.box(xc - 2, -3, -1, xc + 2, -1, -1, t.alt);
        c.box(xc - 2, 0, -1, xc + 2, 0, -1, t.pillar);
        // tezgah sıraları
        int id = 1;
        for (int x = 8; x <= W - 8; x += 8) {
            stall(c, t, x, 6, 0, 1, id++);
            stall(c, t, x, L - 7, 0, -1, id++);
        }
        // takas kabinleri
        int per = 2 + size;
        for (int row = 0; row < 2; row++) {
            int bz = L / 2 + (row == 0 ? -5 : 5);
            for (int k = 0; k < per; k++) {
                int bx = 10 + (int) ((W - 20.0) * k / (per - 1));
                booth(c, t, bx, bz, row * per + k + 1);
            }
        }
        c.setSpawn(W / 2, 1, L / 2);
        c.clearance(14);
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
}
