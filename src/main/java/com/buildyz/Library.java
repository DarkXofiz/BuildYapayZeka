package com.buildyz;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** İsteğe bağlı: plugins/BuildYapayZeka/maps klasöründeki .schem dosyaları. Hazır yapı şablonu yoktur. */
final class Library {

    record Entry(int id, String name, Supplier<Canvas> make) {}

    private final List<Entry> entries = new ArrayList<>();

    Library(File dir, Logger log) {
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".schem"));
        if (files != null) {
            Arrays.sort(files);
            int id = 1;
            for (File f : files) {
                final File ff = f;
                entries.add(new Entry(id++, f.getName(), () -> {
                    try {
                        return Schem.load(ff);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }));
            }
        }
        log.info(entries.size() + " harita dosyası bulundu.");
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
