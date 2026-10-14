package com.zaig100.dargon.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/** Трек из {@code music/<имя>.ogg}, потоковый ({@link Music} libGDX). */
public final class GdxTrack implements MusicPlayer.Track {

    private final Music music;

    public GdxTrack(String name) {
        music = Gdx.audio.newMusic(Gdx.files.internal("music/" + name + ".ogg"));
    }

    @Override
    public void play() {
        music.play();
    }

    @Override
    public void setVolume(float volume) {
        music.setVolume(volume);
    }

    @Override
    public void setLooping(boolean looping) {
        music.setLooping(looping);
    }

    @Override
    public boolean isPlaying() {
        return music.isPlaying();
    }

    @Override
    public void dispose() {
        music.stop();
        music.dispose();
    }
}
