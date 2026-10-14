package com.zaig100.dargon.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.InputMapper;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.ui.Bindings;

/**
 * Контекстная кнопка действия: круг со значком того, что сделает «использовать» (лестница, ключ,
 * предмет…). Если взаимодействовать не с чем — полупрозрачная. Значок меняется по сигналу, а не
 * каждый кадр.
 */
final class ActionButton extends Container<Image> {

    private static final float IDLE_ALPHA = 0.4f;
    private static final float ICON = 0.5f;

    private final GameAssets assets;
    private final HudModel hud;
    private final float alpha;
    private final Image icon = new Image();
    private final Bindings bindings = new Bindings();

    ActionButton(GameAssets assets, InputMapper input, HudModel hud, float size, float alpha) {
        super(new Image());
        this.assets = assets;
        this.hud = hud;
        this.alpha = alpha;
        setActor(icon);
        size(size * ICON);
        setSize(size, size);
        setBackground(new TextureRegionDrawable(assets.region("touch_button")));
        icon.setScaling(Scaling.fit);
        setTouchable(Touchable.enabled);
        addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event, float x, float y, int pointer, int button) {
                        input.actionDown(Action.USE);
                        return true;
                    }

                    @Override
                    public void touchUp(
                            InputEvent event, float x, float y, int pointer, int button) {
                        input.actionUp(Action.USE);
                    }
                });
    }

    @Override
    protected void setStage(Stage stage) {
        if (stage != null && getStage() == null) {
            bindings.observe(
                    hud.actionIcon,
                    region -> {
                        icon.setDrawable(
                                new TextureRegionDrawable(
                                        region == null
                                                ? assets.region("use")
                                                : assets.frame(region, 0)));
                        getColor().a = region == null ? alpha * IDLE_ALPHA : alpha;
                    });
            bindings.observe(
                    hud.actionTint,
                    tint -> icon.setColor(tint == null ? Color.WHITE : Color.valueOf(tint)));
        } else if (stage == null) {
            bindings.dispose();
        }
        super.setStage(stage);
    }
}
