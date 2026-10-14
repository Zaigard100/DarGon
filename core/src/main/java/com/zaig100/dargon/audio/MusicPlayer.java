package com.zaig100.dargon.audio;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Фоновая музыка. В меню по кругу играет один трек; в игре треки уровней идут в случайном порядке,
 * каждый доигрывает до конца (переход на другой уровень его не прерывает), следующий — не тот же,
 * что только что звучал. Смена меню ↔ игра — плавно: текущий трек затихает, новый нарастает.
 */
public final class MusicPlayer {

    /** Что сейчас на экране. */
    public enum Mood {
        MENU,
        GAME
    }

    /** Трек, который умеет играть платформа (на libGDX — {@code Music}). */
    public interface Track {
        void play();

        void setVolume(float volume);

        void setLooping(boolean looping);

        boolean isPlaying();

        void dispose();
    }

    /** Секунды затихания при смене меню ↔ игра и нарастания нового трека. */
    static final float FADE_OUT = 2f;

    static final float FADE_IN = 3f;

    private final Function<String, Track> loader;
    private final String menuTrack;
    private final List<String> levelTracks;
    private final Random random;

    private Track current;
    private String currentName;
    private Mood currentMood;

    /** Громкость текущего трека от затихания/нарастания, 0..1. */
    private float gain;

    public MusicPlayer(
            Function<String, Track> loader,
            String menuTrack,
            List<String> levelTracks,
            Random random) {
        this.loader = loader;
        this.menuTrack = menuTrack;
        this.levelTracks = List.copyOf(levelTracks);
        this.random = random;
    }

    /** Кадр: {@code mood} — меню или игра, {@code volume} — громкость из настроек, 0..1. */
    public void update(float delta, Mood mood, float volume) {
        if (current != null && currentMood != mood) {
            gain -= delta / FADE_OUT;
            if (gain <= 0) {
                stop();
            }
        } else if (current != null && currentMood == Mood.GAME && !current.isPlaying()) {
            // Трек уровня доиграл — следующий, без паузы.
            start(mood, nextLevelTrack());
        } else if (current == null) {
            start(mood, mood == Mood.MENU ? menuTrack : nextLevelTrack());
        } else {
            gain = Math.min(1, gain + delta / FADE_IN);
        }
        if (current != null) {
            current.setVolume(Math.max(0, gain) * volume);
        }
    }

    /** Имя играющего трека или {@code null} (для тестов и отладки). */
    public String playing() {
        return current == null ? null : currentName;
    }

    public void dispose() {
        stop();
    }

    private String nextLevelTrack() {
        if (levelTracks.size() == 1) {
            return levelTracks.get(0);
        }
        String next;
        do {
            next = levelTracks.get(random.nextInt(levelTracks.size()));
        } while (next.equals(currentName));
        return next;
    }

    private void start(Mood mood, String name) {
        stop();
        current = loader.apply(name);
        currentName = name;
        currentMood = mood;
        gain = 0;
        current.setVolume(0);
        current.setLooping(mood == Mood.MENU);
        current.play();
    }

    private void stop() {
        if (current != null) {
            current.dispose();
            current = null;
        }
        // Имя остаётся: следующий трек уровня не повторит только что звучавший.
        currentMood = null;
        gain = 0;
    }
}
