package com.untitledgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Scene2D-based HUD wrapper that keeps world overlays separated from UI layout.
 * Call {@link #update(int, int, float)} and {@link #draw()} each frame to display the HUD.
 */
public final class HudUi implements Disposable {
    private final Stage stage;
    private final Label messageLabel;
    private final HealthBar healthBar;
    private long messageExpireMs;

    public HudUi(BitmapFont font, Texture hbFull, Texture hb75, Texture hb50, Texture hb25, Texture hbZero) {
        ScreenViewport viewport = new ScreenViewport();
        stage = new Stage(viewport);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, font.getColor());
        healthBar = new HealthBar(hbFull, hb75, hb50, hb25, hbZero);

        messageLabel = new Label("", labelStyle);
        messageLabel.setAlignment(Align.right);

        float w = Gdx.graphics.getWidth() * 0.35f;
        float h = Gdx.graphics.getHeight() * 0.20f;

        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(12f);

        root.add(healthBar)
                .left()
                .width(w)
                .height(h);
        root.add(messageLabel).expandX().fillX().right().padLeft(12f);

        stage.addActor(root);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void update(int currentHealth, int maxHealth, float deltaSeconds) {
        healthBar.update(currentHealth, maxHealth);
        if (messageExpireMs > 0 && TimeUtils.millis() > messageExpireMs) {
            clearMessage();
        }
        stage.act(deltaSeconds);
    }

    public void draw() {
        stage.draw();
    }

    public void setMessage(String text, long durationMs) {
        messageLabel.setText(text == null ? "" : text);
        messageExpireMs = durationMs > 0 ? TimeUtils.millis() + durationMs : 0L;
    }

    public void clearMessage() {
        messageLabel.setText("");
        messageExpireMs = 0L;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    private static final class HealthBar extends Image {
        private final TextureRegionDrawable full;
        private final TextureRegionDrawable seventyFive;
        private final TextureRegionDrawable fifty;
        private final TextureRegionDrawable twentyFive;
        private final TextureRegionDrawable empty;

        HealthBar(Texture full, Texture seventyFive, Texture fifty, Texture twentyFive, Texture empty) {
            super(new TextureRegionDrawable(new TextureRegion(full)));
            this.full = new TextureRegionDrawable(new TextureRegion(full));
            this.seventyFive = new TextureRegionDrawable(new TextureRegion(seventyFive));
            this.fifty = new TextureRegionDrawable(new TextureRegion(fifty));
            this.twentyFive = new TextureRegionDrawable(new TextureRegion(twentyFive));
            this.empty = new TextureRegionDrawable(new TextureRegion(empty));
            setScaling(Scaling.stretch);
        }

        void update(int current, int max) {
            if (max <= 0) {
                setDrawable(empty);
                return;
            }
            float pct = Math.max(0f, Math.min(1f, current / (float) max));
            setDrawable(selectDrawable(pct));
        }

        private TextureRegionDrawable selectDrawable(float pct) {
            if (pct >= 0.75f) {
                return full;
            }
            if (pct >= 0.50f) {
                return seventyFive;
            }
            if (pct >= 0.25f) {
                return fifty;
            }
            if (pct > 0f) {
                return twentyFive;
            }
            return empty;
        }
    }
}
