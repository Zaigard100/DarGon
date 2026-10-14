package com.zaig100.dargon.pack;

import com.zaig100.dargon.level.PackDefinition;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Пак-архив {@code .dgpack} (zip). {@code pack.json} — в корне архива или в единственной папке
 * верхнего уровня (так получается, если заархивировать папку пака целиком). Архив открывается на
 * каждое чтение: уровни читаются по одному, а файл не держится открытым.
 */
public final class ZipPack implements PackFiles {

    private final File file;
    private final String prefix;

    /**
     * @throws IOException если это не zip или в нём нет {@code pack.json}
     */
    public ZipPack(File file) throws IOException {
        this.file = file;
        this.prefix = findRoot(file);
    }

    private static String findRoot(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String best = null;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.equals(PackDefinition.FILE)) {
                    return "";
                }
                int slash = name.indexOf('/');
                if (best == null
                        && slash > 0
                        && name.substring(slash + 1).equals(PackDefinition.FILE)) {
                    best = name.substring(0, slash + 1);
                }
            }
            if (best == null) {
                throw new IOException("в архиве нет " + PackDefinition.FILE);
            }
            return best;
        }
    }

    @Override
    public String read(String path) {
        byte[] data = bytes(path);
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] bytes(String path) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry(prefix + path);
            if (entry == null || entry.isDirectory()) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return readAll(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("не читается " + file + ": " + e.getMessage(), e);
        }
    }

    /** Без {@code InputStream.readAllBytes()}: на Android он есть только с API 33. */
    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    @Override
    public String location() {
        return file.getPath();
    }
}
