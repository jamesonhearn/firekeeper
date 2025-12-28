package com.untitledgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.untitledgame.Engine;

public class UntitledGame extends Game {
    private AssetManager assetManager;

    @Override
    public void create() {
        assetManager = new AssetManager();
        setScreen(new Engine(assetManager));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assetManager != null) {
            assetManager.dispose();
            assetManager = null;
        }
    }
}
