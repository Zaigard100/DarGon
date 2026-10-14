package com.zaig100.dargon.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaig100.dargon.audio.MusicPlayer.Mood;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MusicPlayerTest {

    /** Трек-заглушка: громкость и «доиграл» задаются тестом. */
    private static final class FakeTrack implements MusicPlayer.Track {
        final String name;
        float volume;
        boolean looping;
        boolean playing;
        boolean disposed;

        FakeTrack(String name) {
            this.name = name;
        }

        @Override
        public void play() {
            playing = true;
        }

        @Override
        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public void setLooping(boolean looping) {
            this.looping = looping;
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public void dispose() {
            disposed = true;
            playing = false;
        }
    }

    private final List<FakeTrack> loaded = new ArrayList<>();
    private final MusicPlayer music =
            new MusicPlayer(
                    name -> {
                        FakeTrack t = new FakeTrack(name);
                        loaded.add(t);
                        return t;
                    },
                    "menu",
                    List.of("a", "b", "c"),
                    new Random(1));

    private FakeTrack last() {
        return loaded.get(loaded.size() - 1);
    }

    private void run(Mood mood, float seconds) {
        for (float t = 0; t < seconds; t += 0.1f) {
            music.update(0.1f, mood, 1f);
        }
    }

    @Test
    void menuLoopsAndFadesIn() {
        run(Mood.MENU, 0.1f);
        assertEquals("menu", music.playing());
        assertTrue(last().looping);
        assertTrue(last().volume < 0.1f, "начинается тихо");
        run(Mood.MENU, MusicPlayer.FADE_IN + 0.5f);
        assertEquals(1f, last().volume, 1e-4);
    }

    @Test
    void switchToGameFadesOutMenuFirst() {
        run(Mood.MENU, 5);
        FakeTrack menu = last();
        run(Mood.GAME, MusicPlayer.FADE_OUT / 2);
        assertEquals("menu", music.playing(), "меню затихает, а не обрывается");
        assertTrue(menu.volume > 0.3f && menu.volume < 0.7f);
        run(Mood.GAME, MusicPlayer.FADE_OUT);
        assertTrue(menu.disposed);
        assertTrue(List.of("a", "b", "c").contains(music.playing()));
        assertTrue(!last().looping, "трек уровня доигрывает и сменяется");
    }

    @Test
    void levelTrackPlaysToTheEndThenAnotherOne() {
        run(Mood.GAME, 10);
        FakeTrack first = last();
        int count = loaded.size();
        // Смена уровня ничего не меняет: настроение то же — трек продолжает играть.
        run(Mood.GAME, 60);
        assertEquals(count, loaded.size());
        first.playing = false;
        run(Mood.GAME, 0.1f);
        assertNotEquals(first.name, music.playing(), "следующий — другой трек");
        for (int i = 0; i < 20; i++) {
            String before = music.playing();
            last().playing = false;
            run(Mood.GAME, 0.1f);
            assertNotEquals(before, music.playing());
        }
    }
}
