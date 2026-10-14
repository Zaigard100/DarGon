package com.zaig100.dargon.android;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.DarGonGame;
import com.zaig100.dargon.Platform;
import com.zaig100.dargon.pack.PackRepository;
import com.zaig100.dargon.pack.ZipPack;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/** Запуск игры на Android. */
public final class AndroidLauncher extends AndroidApplication {

    private static final int PICK_PACK = 1;

    /** Кому сообщить итог выбора пака (выбор идёт в отдельной активности системы). */
    private Consumer<String> pendingImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        initialize(new DarGonGame(new AndroidPlatform()), config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PACK || pendingImport == null) {
            return;
        }
        Consumer<String> done = pendingImport;
        pendingImport = null;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return; // выбор отменён
        }
        Uri uri = data.getData();
        // Копирование — не в потоке интерфейса, итог — в потоке игры.
        new Thread(
                        () -> {
                            String error = copyPack(uri);
                            Gdx.app.postRunnable(() -> done.accept(error));
                        },
                        "pack-import")
                .start();
    }

    /** Копирует выбранный файл в папку паков; {@code null} — успех, иначе текст ошибки. */
    private String copyPack(Uri uri) {
        FileHandle dir = Gdx.files.local("packs");
        dir.mkdirs();
        String name = displayName(uri);
        if (name == null || !name.toLowerCase().endsWith(PackRepository.EXTENSION)) {
            name = "pack-" + System.currentTimeMillis() + PackRepository.EXTENSION;
        }
        File target = dir.child(name.replaceAll("[\\\\/:*?\"<>|]", "_")).file();
        File temp = new File(target.getPath() + ".part");
        try (InputStream in = getContentResolver().openInputStream(uri);
                OutputStream out = new FileOutputStream(temp)) {
            if (in == null) {
                return "файл не открылся";
            }
            byte[] buffer = new byte[8192];
            for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                out.write(buffer, 0, n);
            }
        } catch (IOException | SecurityException e) {
            temp.delete();
            return e.getMessage();
        }
        try {
            new ZipPack(temp); // это пак: zip с pack.json
        } catch (IOException e) {
            temp.delete();
            return e.getMessage();
        }
        if (target.exists()) {
            target.delete();
        }
        return temp.renameTo(target) ? null : "не удалось сохранить файл";
    }

    private String displayName(Uri uri) {
        try (Cursor cursor =
                getContentResolver()
                        .query(
                                uri,
                                new String[] {OpenableColumns.DISPLAY_NAME},
                                null,
                                null,
                                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (RuntimeException ignored) {
            // Имя не узнать — файл получит своё.
        }
        return null;
    }

    private final class AndroidPlatform implements Platform {
        @Override
        public String name() {
            return "android";
        }

        @Override
        public boolean isTouchPrimary() {
            return true;
        }

        @Override
        public FileHandle dataDir() {
            return Gdx.files.local("saves");
        }

        @Override
        public FileHandle packsDir() {
            return Gdx.files.local("packs");
        }

        @Override
        public boolean canImportPacks() {
            return true;
        }

        @Override
        public void importPack(Consumer<String> done) {
            runOnUiThread(
                    () -> {
                        pendingImport = done;
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        // У .dgpack нет своего MIME-типа: показываем все файлы.
                        intent.setType("*/*");
                        startActivityForResult(intent, PICK_PACK);
                    });
        }

        @Override
        public void vibrate(int milliseconds) {
            Gdx.input.vibrate(milliseconds);
        }
    }
}
