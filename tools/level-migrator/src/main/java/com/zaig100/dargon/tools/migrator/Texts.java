package com.zaig100.dargon.tools.migrator;

import java.util.Map;

/** Тексты старых уровней: исправленный английский вариант и перевод на русский. */
final class Texts {

    private Texts() {}

    /** Опечатки в старых названиях уровней. */
    static final Map<String, String> ENGLISH_FIXES =
            Map.of(
                    "Dangerouse", "Dangerous",
                    "Tiwins", "Twins",
                    "Old 15(Crashed!!!)", "Old 15 (Crashed!!!)",
                    "DOWN:Next", "DOWN: Next");

    static final Map<String, String> RUSSIAN =
            Map.ofEntries(
                    Map.entry("First", "Первый"),
                    Map.entry("Dangerous", "Опасный"),
                    Map.entry("Teleporters", "Телепорты"),
                    Map.entry("TurboFly", "Турбомуха"),
                    Map.entry("NoFire", "Без огня"),
                    Map.entry("Arr OOW", "Стрелааа"),
                    Map.entry("EnderMan", "Эндермен"),
                    Map.entry("Old 15 (Crashed!!!)", "Старый 15 (сломан!!!)"),
                    Map.entry("Infection", "Заражение"),
                    Map.entry("Limo", "Лимо"),
                    Map.entry("Twins", "Близнецы"),
                    Map.entry("Ktulhu", "Ктулху"),
                    Map.entry("Twins II", "Близнецы II"),
                    Map.entry("Eich", "Эйх"),
                    Map.entry("Prison", "Тюрьма"),
                    Map.entry("Dung-Dung", "Данг-Данг"),
                    Map.entry("Dung-Dung II", "Данг-Данг II"),
                    Map.entry("Stability", "Стабильность"),
                    Map.entry("Minimalism", "Минимализм"),
                    Map.entry("One Way", "В одну сторону"),
                    Map.entry("SWay", "S-путь"),
                    Map.entry("Again", "Снова"),
                    Map.entry("II DOOR", "II ДВЕРИ"),
                    Map.entry("Invers", "Инверсия"),
                    Map.entry("Why?", "Зачем?"),
                    Map.entry("Bastion", "Бастион"),
                    Map.entry("RunForLive", "Беги, чтобы жить"),
                    Map.entry("Shop", "Лавка"),
                    Map.entry("All levels", "Все уровни"),
                    Map.entry("UP: Shop\nDOWN: Next", "ВВЕРХ: лавка\nВНИЗ: дальше"),
                    Map.entry("< Shop\nNext >", "< Лавка\nДальше >"),
                    Map.entry("I told\nYOU", "Я же\nГОВОРИЛ"),
                    Map.entry("Go\nDown", "Иди\nвниз"),
                    Map.entry("Go\nUp", "Иди\nвверх"));

    /** Исправляет опечатки построчно. */
    static String english(String legacy) {
        String[] lines = legacy.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = ENGLISH_FIXES.getOrDefault(lines[i], lines[i]);
        }
        return String.join("\n", lines);
    }

    /** Перевод; для строк без букв (схемы из символов, тестовые уровни) — сам текст. */
    static String russian(String english) {
        String ru = RUSSIAN.get(english);
        return ru != null ? ru : english;
    }

    /** Есть ли перевод: тексты из букв без перевода попадают в отчёт. */
    static boolean hasRussian(String english) {
        return RUSSIAN.containsKey(english) || !english.matches("(?s).*[A-Za-z]{2,}.*");
    }
}
