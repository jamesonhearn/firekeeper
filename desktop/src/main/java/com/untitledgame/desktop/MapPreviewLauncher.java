package com.untitledgame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.untitledgame.debug.MapPreviewApp;

/**
 * Standalone launcher for the map previewer. Usage:
 * <pre>
 *   ./gradlew desktop:run -PmainClass=com.untitledgame.desktop.MapPreviewLauncher --args="12345"
 * </pre>
 * If no seed is provided, a random seed is used.
 */
public class MapPreviewLauncher {
    public static void main(String[] arg) {
        long seed = arg.length > 0 ? parseSeed(arg[0]) : System.currentTimeMillis();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Map Preview");
        config.setWindowedMode(800, 600);
        new Lwjgl3Application(new MapPreviewApp(seed), config);
    }

    private static long parseSeed(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }
}