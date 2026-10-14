package com.zaig100.dargon.save;

import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.level.GameData;

/**
 * Три слота сохранений. Если основной файл слота повреждён, читается резервная копия (предыдущее
 * сохранение). «Продолжить» берёт последний использованный слот.
 */
public final class SaveSlots {

    public static final int COUNT = 3;

    private static final String LAST = "last-slot";

    /** Состояние слота для экрана выбора. */
    public record Slot(int index, Progress progress, long updated, String error) {
        public boolean isEmpty() {
            return progress == null && error == null;
        }

        public boolean isBroken() {
            return error != null;
        }
    }

    private final Storage storage;
    private final GameData data;

    public SaveSlots(Storage storage, GameData data) {
        this.storage = storage;
        this.data = data;
    }

    private static String file(int slot) {
        return "slot" + (slot + 1) + ".json";
    }

    /** Слот: прогресс, пустой или ошибка (если повреждены и основной файл, и копия). */
    public Slot slot(int index) {
        String text = storage.read(file(index));
        if (text == null) {
            return new Slot(index, null, 0, null);
        }
        try {
            return new Slot(index, SaveCodec.read(text, data), SaveCodec.updated(text), null);
        } catch (SaveException e) {
            String backup = storage.read(file(index) + ".bak");
            if (backup != null) {
                try {
                    return new Slot(
                            index, SaveCodec.read(backup, data), SaveCodec.updated(backup), null);
                } catch (SaveException ignored) {
                    // Копия тоже повреждена — сообщаем об ошибке основного файла.
                }
            }
            return new Slot(index, null, 0, e.getMessage());
        }
    }

    public void save(int index, Progress progress, long now) {
        storage.write(file(index), SaveCodec.write(progress, now));
        storage.write(LAST, String.valueOf(index));
    }

    public void delete(int index) {
        storage.delete(file(index));
    }

    /** Последний использованный непустой слот или -1. */
    public int last() {
        String text = storage.read(LAST);
        int index;
        try {
            index = text == null ? -1 : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            index = -1;
        }
        if (index >= 0 && index < COUNT && slot(index).progress() != null) {
            return index;
        }
        long best = -1;
        int found = -1;
        for (int i = 0; i < COUNT; i++) {
            Slot slot = slot(i);
            if (slot.progress() != null && slot.updated() > best) {
                best = slot.updated();
                found = i;
            }
        }
        return found;
    }

    /** Первый пустой слот или -1. */
    public int firstEmpty() {
        for (int i = 0; i < COUNT; i++) {
            if (slot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
