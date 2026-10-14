package com.zaig100.dargon.save;

import com.badlogic.gdx.files.FileHandle;

/**
 * Файлы в каталоге: запись через временный файл и переименование (обрыв записи не портит прежний
 * файл), предыдущая версия — в {@code .bak}.
 */
public final class FileStorage implements Storage {

    private static final String TEMP = ".tmp";
    private static final String BACKUP = ".bak";

    private final FileHandle dir;

    public FileStorage(FileHandle dir) {
        this.dir = dir;
    }

    @Override
    public String read(String name) {
        FileHandle file = dir.child(name);
        return file.exists() ? file.readString("UTF-8") : null;
    }

    @Override
    public void write(String name, String content) {
        dir.mkdirs();
        FileHandle temp = dir.child(name + TEMP);
        temp.writeString(content, false, "UTF-8");
        FileHandle file = dir.child(name);
        if (file.exists()) {
            FileHandle backup = dir.child(name + BACKUP);
            if (backup.exists()) {
                backup.delete();
            }
            file.moveTo(backup);
        }
        temp.moveTo(file);
    }

    @Override
    public void delete(String name) {
        dir.child(name).delete();
        dir.child(name + BACKUP).delete();
    }
}
