package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.kotcrab.vis.ui.widget.VisList;
import java.util.function.IntFunction;

/** Список строк с картинкой слева от каждой (префабы и сущности уровня). */
final class IconList extends VisList<String> {

    private final IntFunction<TextureRegion> icon;

    /**
     * @param icon картинка строки по номеру; {@code null} — без картинки (место остаётся)
     */
    IconList(IntFunction<TextureRegion> icon) {
        this.icon = icon;
    }

    @Override
    protected GlyphLayout drawItem(
            Batch batch, BitmapFont font, int index, String item, float x, float y, float width) {
        float size = getItemHeight() - 2;
        float top = y + getStyle().selection.getTopHeight() - font.getDescent();
        TextureRegion region = icon.apply(index);
        if (region != null) {
            // Высокие картинки (дверь 16×32) — вписать по высоте.
            float scale = size / Math.max(region.getRegionWidth(), region.getRegionHeight());
            float w = region.getRegionWidth() * scale;
            float h = region.getRegionHeight() * scale;
            float color = batch.getPackedColor();
            batch.setColor(1, 1, 1, 1);
            batch.draw(region, x + (size - w) / 2, top - 1 - size + (size - h) / 2, w, h);
            batch.setPackedColor(color);
        }
        return super.drawItem(batch, font, index, item, x + size + 6, y, width - size - 6);
    }
}
