package com.buildyz;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** 1-150 arası hazır yapılar (5 tür x 10 tema x 3 boyut). 151 ve sonrası maps klasöründeki .schem dosyaları. */
final class Library {

    record Entry(int id, String name, Supplier<Canvas> make) {}

    private final List<Entry> entries = new ArrayList<>();

    Library(File dir, Logger log) {
        String[] sz = {"S", "M", "L"};
        int id = 1;
        for (Structures.Kind k : Structures.Kind.values()) {
            for (Theme th : Theme.values()) {
                for (int s = 0; s < 3; s++) {
                    final int fid = id;
                    final int fs = s;
                    final Structures.Kind fk = k;
                    final Theme fth = th;
                    entries.add(new Entry(id, k.label + " " + th.label + " " + sz[s],
                            () -> Structures.build(fk, fth, fs, fid * 7919L)));
                    id++;
                }
            }
        }
        int generated = entries.size();
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".schem"));
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                final File ff = f;
                entries.add(new Entry(id++, "Harita: " + f.getName(), () -> {
                    try {
                        return Schem.load(ff);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }));
            }
        }
        log.info(generated + " hazır yapı ve " + (entries.size() - generated) + " harita dosyası yüklendi.");
    }

    int size() {
        return entries.size();
    }

    Entry get(int id) {
        return id >= 1 && id <= entries.size() ? entries.get(id - 1) : null;
    }

    List<Entry> all() {
        return entries;
    }
}
