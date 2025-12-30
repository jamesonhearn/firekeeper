package com.untitledgame.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.untitledgame.ui.UiFont;

import java.util.EnumMap;

public final class UiAssets implements Disposable {

    private final EnumMap<UiFont, BitmapFont> fonts = new EnumMap<>(UiFont.class);

    public void load() {
        FreeTypeFontGenerator gen =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/ui.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();

        // title font
        p.size = 48;
        p.color = Color.WHITE;
        fonts.put(UiFont.TITLE, gen.generateFont(p));

        // body font
        p.size = 24;
        fonts.put(UiFont.BODY, gen.generateFont(p));

        gen.dispose();
    }

    public BitmapFont font(UiFont font) {
        return fonts.get(font);
    }

    @Override
    public void dispose() {
        for (BitmapFont font : fonts.values()) {
            font.dispose();
        }
    }
}