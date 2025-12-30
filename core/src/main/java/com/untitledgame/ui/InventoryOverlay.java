package com.untitledgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.untitledgame.logic.items.Inventory;
import com.untitledgame.logic.items.ItemStack;

import java.util.List;

/**
 * Scene2D overlay for the inventory, displaying slots in order with libGDX UI widgets.
 */
public final class InventoryOverlay implements Disposable {
    private final Stage stage;
    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle bodyStyle;
    private final Table slotsTable;

    public InventoryOverlay(BitmapFont titleFont, BitmapFont bodyFont) {
        stage = new Stage(new ScreenViewport());
        titleStyle = new Label.LabelStyle(titleFont, titleFont.getColor());
        bodyStyle = new Label.LabelStyle(bodyFont, bodyFont.getColor());

        Label title = new Label("Inventory (press I to close)", titleStyle);
        title.setAlignment(Align.center);

        slotsTable = new Table();
        slotsTable.defaults().pad(4f).left();

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(12f);
        root.add(title).expandX().center().row();
        root.add(slotsTable).expand().top().left();
        stage.addActor(root);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void render(Inventory inventory) {
        rebuildSlots(inventory);
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

    private void rebuildSlots(Inventory inventory) {
        slotsTable.clearChildren();
        List<ItemStack> slots = inventory.slots();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            String labelText = stack == null || stack.quantity() <= 0
                    ? String.format("%02d: (empty)", i + 1)
                    : String.format("%02d: %s x%d", i + 1, stack.item().name(), stack.quantity());
            Label row = new Label(labelText, bodyStyle);
            row.setAlignment(Align.left);
            slotsTable.add(row).left().row();
        }
    }
}