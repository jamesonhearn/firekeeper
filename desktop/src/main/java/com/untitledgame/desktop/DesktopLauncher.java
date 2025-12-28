package com.untitledgame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.untitledgame.UntitledGame;

public final class DesktopLauncher {
    private DesktopLauncher() {
        // Utility class
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("UntitledGame");
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new UntitledGame(), config);
    }
}
