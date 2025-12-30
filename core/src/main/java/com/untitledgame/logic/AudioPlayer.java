package com.untitledgame.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioPlayer {
    private final AssetManager assets;
    private final List<String> effectPaths = new ArrayList<>();
    private final Random random = new Random();
    private float soundVolume = 0.2f;
    private float musicVolume = 0.1f;
    private boolean muted = false;
    private Music currentMusic;

    public AudioPlayer(AssetManager assets) {
        this.assets = assets;
    }

    public void loadEffects(String... paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            if (!assets.isLoaded(path)) {
                assets.load(path, Sound.class);
            }
            effectPaths.add(path);
        }
    }

    public void playRandomEffect() {
        if (muted || effectPaths.isEmpty()) {
            return;
        }
        String choice = effectPaths.get(random.nextInt(effectPaths.size()));
        Sound sound = assets.get(choice, Sound.class);
        sound.play(soundVolume);
    }

    public void play(String path) {
        playInternal(path, false, null);
    }

    public void playLoop(String path) {
        playInternal(path, true, null);
    }

    public void playThenCallback(String path, Runnable callback) {
        playInternal(path, false, callback);
    }

    public void stop() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    public void setSoundVolume(float volume) {
        soundVolume = clamp(volume);
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        if (currentMusic != null) {
            currentMusic.setVolume(muted ? 0f : musicVolume);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (currentMusic != null) {
            currentMusic.setVolume(muted ? 0f : musicVolume);
        }
    }

    private void playInternal(String path, boolean loop, Runnable callback) {
        if (path == null || path.isBlank()) {
            return;
        }
        // Music should already be queued during initialization (see Engine.queueAssetLoads)
        if (!assets.isLoaded(path)) {
            // This should NEVER happen in normal operation since all music is pre-queued
            Gdx.app.error("AudioPlayer", "Music not pre-loaded: " + path +
                    " - This indicates a bug in asset initialization. Loading synchronously as fallback.");
            // Fallback: load synchronously to avoid silent failure
            // While this blocks the render thread, it only happens on bugs/edge cases
            // and is better than having no music at all
            assets.load(path, Music.class);
            assets.finishLoadingAsset(path);
        }
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = assets.get(path, Music.class);
        currentMusic.setLooping(loop);
        currentMusic.setVolume(muted ? 0f : musicVolume);
        currentMusic.play();
        if (callback != null) {
            currentMusic.setOnCompletionListener(music -> callback.run());
        }
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}

