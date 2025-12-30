package com.untitledgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Reusable Scene2D overlay for centered menu/pause/death/end screens.
 * Keeps text layout responsive to screen size and leverages libGDX UI primitives.
 */
public final class ScreenOverlay implements Disposable {
    private final Stage stage;
    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle bodyStyle;
    private final Label titleLabel;
    private final Table contentTable;

    public ScreenOverlay(BitmapFont titleFont, BitmapFont bodyFont) {
        titleFont.getData().setScale(2.0f);
        bodyFont.getData().setScale(1.0f);
        stage = new Stage(new ScreenViewport());

        titleStyle = new Label.LabelStyle(titleFont, titleFont.getColor());
        bodyStyle = new Label.LabelStyle(bodyFont, bodyFont.getColor());

        titleLabel = new Label("", this.titleStyle);
        titleLabel.setAlignment(Align.center);

        contentTable = new Table();
        contentTable.center().defaults().padTop(8f);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.add(titleLabel).padBottom(16f).row();
        root.add(contentTable).center();
        stage.addActor(root);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void renderCentered(String title, String[] lines) {
        titleLabel.setText(title == null ? "" : title);
        contentTable.clearChildren();
        if (lines != null) {
            for (String line : lines) {
                Label label = new Label(line, bodyStyle);
                label.setAlignment(Align.center);
                contentTable.add(label).center().row();
            }
        }
        float delta = Gdx.graphics.getDeltaTime();
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
