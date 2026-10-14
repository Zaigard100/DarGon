package com.zaig100.dargon.pack;

import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.PackDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Паки, доступные игре: встроенный (в ассетах) и установленные — каталоги и архивы {@code .dgpack}
 * в папке паков. Сломанный пак не роняет список: он попадает в него с причиной.
 */
public final class PackRepository {

    /** Расширение архива пака. */
    public static final String EXTENSION = ".dgpack";

    /** Пак в списке. {@code info} — {@code null}, если {@code pack.json} не прочитался. */
    public record Entry(PackFiles files, PackDefinition.Info info, boolean builtin, String error) {

        public String id() {
            return info == null ? null : info.id();
        }

        /** Можно ли попробовать его загрузить (совместимость проверяется отдельно). */
        public boolean readable() {
            return error == null;
        }
    }

    private final FileHandle builtinRoot;
    private final FileHandle userDir;

    /**
     * @param builtinRoot каталог встроенных паков в ассетах ({@code packs/})
     * @param userDir папка установленных паков
     */
    public PackRepository(FileHandle builtinRoot, FileHandle userDir) {
        this.builtinRoot = builtinRoot;
        this.userDir = userDir;
    }

    /** Папка установленных паков. */
    public FileHandle userDir() {
        return userDir;
    }

    /** Встроенный пак первым, затем установленные по имени файла. */
    public List<Entry> list() {
        List<Entry> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        result.add(entry(new DirectoryPack(builtinRoot.child(GameData.BUILTIN_PACK)), true, ids));
        if (userDir.isDirectory()) {
            FileHandle[] children = userDir.list();
            Arrays.sort(children, Comparator.comparing(FileHandle::name));
            for (FileHandle child : children) {
                Entry e = open(child, ids);
                if (e != null) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    /** Пак с этим id, который читается; {@code null}, если такого нет. */
    public Entry find(String id) {
        for (Entry e : list()) {
            if (e.readable() && id.equals(e.id())) {
                return e;
            }
        }
        return null;
    }

    private static Entry open(FileHandle child, Set<String> ids) {
        if (child.isDirectory()) {
            if (!child.child(PackDefinition.FILE).exists()) {
                return null;
            }
            return entry(new DirectoryPack(child), false, ids);
        }
        String name = child.name().toLowerCase();
        if (!name.endsWith(EXTENSION) && !name.endsWith(".zip")) {
            return null;
        }
        try {
            return entry(new ZipPack(child.file()), false, ids);
        } catch (IOException | RuntimeException e) {
            return new Entry(new BrokenPack(child.path()), null, false, e.getMessage());
        }
    }

    private static Entry entry(PackFiles files, boolean builtin, Set<String> ids) {
        try {
            String source = files.read(PackDefinition.FILE);
            if (source == null) {
                return new Entry(files, null, builtin, "нет " + PackDefinition.FILE);
            }
            PackDefinition.Info info = PackDefinition.header(source, PackDefinition.FILE);
            if (info.id().isEmpty()) {
                return new Entry(files, info, builtin, "в " + PackDefinition.FILE + " нет id");
            }
            if (!ids.add(info.id())) {
                return new Entry(files, info, builtin, "id '" + info.id() + "' уже занят");
            }
            return new Entry(files, info, builtin, null);
        } catch (LevelFormatException | IllegalStateException e) {
            return new Entry(files, null, builtin, e.getMessage());
        }
    }

    /** Архив, который не открылся: в списке он есть, читать из него нечего. */
    private record BrokenPack(String location) implements PackFiles {
        @Override
        public String read(String path) {
            return null;
        }

        @Override
        public byte[] bytes(String path) {
            return null;
        }
    }
}
