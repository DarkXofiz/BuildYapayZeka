package com.buildyz;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Sponge .schem (v2 ve v3) okuyucu. WorldEdit gerekmez. Hava blokları atlanır. */
final class Schem {

    private Schem() {}

    @SuppressWarnings("unchecked")
    static Canvas load(File f) throws IOException {
        Map<String, Object> root;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))))) {
            int type = in.readByte();
            if (type != 10) throw new IOException("Geçersiz schematic dosyası");
            in.readUTF();
            root = (Map<String, Object>) read(in, 10);
        }
        Map<String, Object> s = root.get("Schematic") instanceof Map<?, ?> m ? (Map<String, Object>) m : root;
        int w = num(s.get("Width")), h = num(s.get("Height")), l = num(s.get("Length"));
        Map<String, Object> pal;
        byte[] data;
        if (s.get("Blocks") instanceof Map<?, ?> b) {
            pal = (Map<String, Object>) b.get("Palette");
            data = (byte[]) b.get("Data");
        } else {
            pal = (Map<String, Object>) s.get("Palette");
            data = (byte[]) s.get("BlockData");
        }
        if (pal == null || data == null) throw new IOException("Palet bulunamadı");
        Map<Integer, BlockData> bd = new HashMap<>();
        for (Map.Entry<String, Object> e : pal.entrySet()) {
            BlockData d;
            try {
                d = Bukkit.createBlockData(e.getKey());
            } catch (IllegalArgumentException ex) {
                d = Material.STONE.createBlockData();
            }
            bd.put(num(e.getValue()), d);
        }
        Canvas c = new Canvas();
        int p = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < l; z++) {
                for (int x = 0; x < w; x++) {
                    int v = 0, shift = 0;
                    while (true) {
                        int b = data[p++];
                        v |= (b & 0x7F) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }
                    BlockData d = bd.get(v);
                    if (d == null || d.getMaterial().isAir()) continue;
                    c.put(x, y, z, d, null);
                }
            }
        }
        return c;
    }

    private static int num(Object o) {
        return ((Number) o).intValue();
    }

    private static Object read(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1:
                return in.readByte();
            case 2:
                return in.readShort();
            case 3:
                return in.readInt();
            case 4:
                return in.readLong();
            case 5:
                return in.readFloat();
            case 6:
                return in.readDouble();
            case 7: {
                byte[] a = new byte[in.readInt()];
                in.readFully(a);
                return a;
            }
            case 8:
                return in.readUTF();
            case 9: {
                int t = in.readByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < n; i++) list.add(read(in, t));
                return list;
            }
            case 10: {
                Map<String, Object> m = new HashMap<>();
                while (true) {
                    int t = in.readByte();
                    if (t == 0) break;
                    String name = in.readUTF();
                    m.put(name, read(in, t));
                }
                return m;
            }
            case 11: {
                int[] a = new int[in.readInt()];
                for (int i = 0; i < a.length; i++) a[i] = in.readInt();
                return a;
            }
            case 12: {
                long[] a = new long[in.readInt()];
                for (int i = 0; i < a.length; i++) a[i] = in.readLong();
                return a;
            }
            default:
                throw new IOException("Bilinmeyen NBT tipi " + type);
        }
    }
}
