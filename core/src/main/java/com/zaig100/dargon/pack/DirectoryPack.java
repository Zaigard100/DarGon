package com.zaig100.dargon.pack;

import com.badlogic.gdx.files.FileHandle;

/** Пак-каталог: встроенный (в ассетах) или распакованный в папке паков. */
public final class DirectoryPack implements PackFiles {

    private final FileHandle root;

    public DirectoryPack(FileHandle root) {
        this.root = root;
    }

    @Override
    public String read(String path) {
        FileHandle file = root.child(path);
        return file.exists() ? file.readString("UTF-8") : null;
    }

    @Override
    public byte[] bytes(String path) {
        FileHandle file = root.child(path);
        return file.exists() ? file.readBytes() : null;
    }

    @Override
    public String location() {
        return root.path();
    }
}
