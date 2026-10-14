package com.zaig100.dargon.editor.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Упаковка каталога пака в {@code .dgpack}: zip с {@code pack.json} в корне. */
public final class PackExporter {

    private PackExporter() {}

    /** Пакует {@code dir} в {@code target}; временные и скрытые файлы пропускаются. */
    public static void export(Path dir, Path target) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".part");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files =
                    walk.filter(Files::isRegularFile)
                            .filter(f -> include(dir, f))
                            .sorted()
                            .toList();
        }
        try (OutputStream out = Files.newOutputStream(temp);
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Path file : files) {
                zip.putNextEntry(new ZipEntry(dir.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean include(Path dir, Path file) {
        for (Path part : dir.relativize(file)) {
            String name = part.toString();
            if (name.startsWith(".")) {
                return false;
            }
        }
        String name = file.getFileName().toString();
        return !name.endsWith(".tmp") && !name.endsWith(".part") && !name.endsWith("~");
    }
}
