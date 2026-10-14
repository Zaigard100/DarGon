package com.zaig100.dargon.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dargon.game.GameSession;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.render.LevelRenderer;
import com.zaig100.dargon.render.PixelScreen;

/**
 * Фон меню, как в DarGon3: первый уровень с героем, который стоит у входа. Уровень живёт
 * (анимации), но без управления, без темноты и приглушён, чтобы меню читалось.
 */
final class MenuBackground {

    /**
     * Уровень под меню и клетка героя на нём. Как в DarGon3, герой стоит у правого края кадра во
     * втором ряду сверху — сбоку от меню; над ним коридор верхнего ряда.
     */
    private static final String LEVEL = "01";

    private static final int HERO_X = 7;
    private static final int HERO_Y = 1;
    private static final int HERO_COLUMN = 6;
    private static final int HERO_ROW = 1;

    /** Насколько приглушить уровень под меню. */
    private static final float DIM = 0.45f;

    private final GameAssets assets;
    private final LevelRuntime level;
    private final LevelRenderer renderer;
    private float accumulator;

    MenuBackground(GameData data, GameAssets assets, boolean newYear) {
        this.assets = assets;
        GameSession session = new GameSession(data, new ActionState());
        level = session.enter(LEVEL, PlayerState.newGame(data.items()));
        level.state().setDark(false);
        level.world().get(level.player(), GridPos.class).set(HERO_X, HERO_Y);
        level.world().get(level.player(), Motion.class).snap();
        renderer = new LevelRenderer(assets, data.items(), newYear);
        renderer.bind(level.world());
        renderer.setHeroCell(HERO_COLUMN, HERO_ROW);
    }

    void update(float delta) {
        accumulator += Math.min(delta, 0.25f);
        while (accumulator >= LevelRuntime.TICK) {
            accumulator -= LevelRuntime.TICK;
            level.tick();
        }
    }

    void draw(SpriteBatch batch, PixelScreen screen) {
        screen.beginWorld(batch);
        renderer.render(batch, level, accumulator / LevelRuntime.TICK);
        batch.setColor(1, 1, 1, DIM);
        batch.draw(
                assets.region("black"), 0, 0, LevelRenderer.VIEW_WIDTH, LevelRenderer.VIEW_HEIGHT);
        batch.setColor(1, 1, 1, 1);
        screen.endWorld(batch);
    }

    void dispose() {
        level.dispose();
    }
}
