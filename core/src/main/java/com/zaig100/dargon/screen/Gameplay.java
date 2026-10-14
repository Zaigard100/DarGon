package com.zaig100.dargon.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dargon.ecs.Entity;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.GameSession;
import com.zaig100.dargon.game.LevelOutcome;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayStats;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.item.PlayerActions;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GridPoint;
import com.zaig100.dargon.render.LevelRenderer;
import com.zaig100.dargon.render.PixelScreen;

/**
 * Идущая игра: логика с фиксированным шагом {@link LevelRuntime#TICK}, отрисовка мира с
 * интерполяцией, события уровня (смерть, выход, магазин) → экраны интерфейса.
 */
public final class Gameplay {

    /** Больше этого за кадр не догоняем (после паузы окна, отладчика). */
    private static final float MAX_FRAME = 0.25f;

    private static final int HURT_VIBRATION = 60;
    private static final int DEATH_VIBRATION = 200;

    private final App app;
    private final GameSession session;
    private final ActionState input;
    private final LevelRenderer renderer;
    private final HudModel hud = new HudModel();

    /** Статистика прохождений с инструментами разработчика никуда не пишется. */
    private final PlayStats discarded = new PlayStats();

    private LevelRuntime level;
    private PlayerActions actions;
    private float accumulator;
    private boolean leftSpawn;

    /** Время на текущем уровне (без пауз) и учтено ли оно уже в статистике. */
    private double levelTime;

    /** Прошлые значения щита и монет (заметить поломку и подбор). */
    private int lastShield;

    private int lastCoins;

    private boolean timeAccounted;
    private boolean deathShown;

    Gameplay(App app, GameSession session, ActionState input, LevelRenderer renderer) {
        this.app = app;
        this.session = session;
        this.input = input;
        this.renderer = renderer;
    }

    void start(LevelRuntime runtime) {
        if (level != null && !timeAccounted) {
            stats().left(levelId(), levelTime);
        }
        level = runtime;
        levelTime = 0;
        timeAccounted = false;
        stats().entered(levelId());
        app.achievements().bind(session.progress());
        app.achievements().levelStarted();
        lastShield = runtime.health().shield;
        lastCoins = runtime.state().player().coins;
        app.saveStats();
        actions = new PlayerActions(runtime.world(), session.data().items());
        accumulator = 0;
        leftSpawn = false;
        deathShown = false;
        renderer.bind(runtime.world());
        runtime.world()
                .events()
                .subscribe(
                        GameEvents.Hurt.class,
                        hurt -> {
                            app.sound("lov");
                            app.achievements().damaged(hurt.damage());
                            app.vibrate(hurt.died() ? DEATH_VIBRATION : HURT_VIBRATION);
                            if (hurt.died()) {
                                app.sound("wasted");
                            }
                        });
        runtime.world().events().subscribe(GameEvents.EasterEgg.class, app::easterEgg);
        runtime.world()
                .events()
                .subscribe(GameEvents.Achieve.class, a -> app.achievements().scripted(a.id()));
        hud.sync(level, leftSpawn, session.data().items());
        hud.cheats.set(session.progress().cheats());
    }

    public HudModel hud() {
        return hud;
    }

    public PlayerActions actions() {
        return actions;
    }

    public LevelRuntime level() {
        return level;
    }

    public GameSession session() {
        return session;
    }

    void update(float delta, boolean paused) {
        if (paused) {
            // Действия из инвентаря (пасхалки) доставляются и при стоящем мире.
            level.world().sync();
            accumulator = 0;
        } else {
            delta *= session.cheats().timeScale;
            accumulator += Math.min(delta, MAX_FRAME * session.cheats().timeScale);
            if (!level.isPlayerDead()) {
                session.addPlayTime(Math.min(delta, MAX_FRAME));
                levelTime += Math.min(delta, MAX_FRAME);
            }
            while (accumulator >= LevelRuntime.TICK) {
                accumulator -= LevelRuntime.TICK;
                level.tick();
                if (afterTick()) {
                    break;
                }
            }
        }
        hud.sync(level, leftSpawn, session.data().items());
        hud.cheats.set(session.progress().cheats());
    }

    /** Реакция на тик; {@code true} — уровень сменился или открылся экран. */
    /** Щит сломался, монеты подобраны — для достижений. */
    private void watch() {
        int shield = level.health().shield;
        if (lastShield > 0 && shield == 0) {
            app.achievements().shieldBroken();
        }
        lastShield = shield;
        int coins = level.state().player().coins;
        if (coins > lastCoins) {
            app.achievements().coinsCollected(coins - lastCoins);
        }
        lastCoins = coins;
    }

    private boolean afterTick() {
        watch();
        GridPos at = level.playerPos();
        GridPoint spawn = level.state().definition().spawn();
        leftSpawn |= !at.is(spawn.x(), spawn.y());
        if (input.wasPressed(Action.DEBUG)) {
            app.toggleDebug();
        }
        if (level.isPlayerDead()) {
            if (!deathShown) {
                deathShown = true;
                app.achievements().died(level.health().lastDamageKind);
                Progress.Death death = session.died();
                stats().died(levelId(), level.health().lastDamageKind, levelTime);
                timeAccounted = true;
                app.saveStats();
                app.screens().push(new WastedScreen(death, session.progress().attemptsLeft()));
            }
            return false;
        }
        if (input.wasPressed(Action.INVENTORY)) {
            app.screens().push(new InventoryScreen());
            return true;
        }
        if (input.wasPressed(Action.PAUSE)) {
            app.screens().push(new PauseScreen());
            return true;
        }
        if (level.state().openShop() != Entity.NONE) {
            app.screens().push(new ShopScreen(level.state().openShop()));
            return true;
        }
        LevelOutcome outcome = level.outcome();
        if (outcome.kind() == LevelOutcome.Kind.EXIT) {
            stats().completed(levelId(), levelTime);
            app.achievements().levelCompleted(levelId());
            timeAccounted = true;
            start(session.advance());
            return true;
        }
        if (outcome.kind() == LevelOutcome.Kind.GAME_END) {
            app.achievements().levelCompleted(levelId());
            app.achievements().gameCompleted();
            app.screens().push(new EndScreen());
            return true;
        }
        return false;
    }

    /** Перезапуск текущего уровня с тем, с чем на него вошли (в хардкоре — контрольная точка). */
    void restartLevel() {
        start(session.restart());
    }

    /** После смерти: уровень заново или контрольная точка — как решено при смерти. */
    void afterDeath() {
        start(session.afterDeath());
    }

    void draw(SpriteBatch batch, PixelScreen screen, boolean paused) {
        float alpha = paused ? 1f : accumulator / LevelRuntime.TICK;
        screen.beginWorld(batch);
        renderer.render(batch, level, alpha);
        screen.endWorld(batch);
        renderer.renderDarkness(batch, screen, level);
        screen.beginOverlay(batch);
        renderer.renderOverlay(batch, level);
        screen.endOverlay(batch);
    }

    /** Клик по полю (логические координаты игрового окна, Y вниз). */
    void click(float x, float y) {
        input.click(new GridPoint(renderer.cellX(x), renderer.cellY(y)));
    }

    /** Использовать предмет (с учётом в статистике). */
    public boolean use(int slot) {
        var stack = level.state().player().inventory.get(slot);
        boolean used = actions.use(slot);
        if (used && stack != null) {
            stats().used(levelId(), stack.id());
            app.achievements().itemUsed(stack.id());
        }
        return used;
    }

    /** Купить в магазине (с учётом для достижений). */
    public boolean buy(int shop, int offer) {
        boolean bought = actions.buy(shop, offer);
        if (bought) {
            lastCoins = level.state().player().coins;
            app.achievements().purchased();
        }
        return bought;
    }

    /** Статистика: для прохождений с инструментами разработчика — в никуда. */
    private PlayStats stats() {
        return session.progress().cheats() ? discarded : app.stats();
    }

    private String levelId() {
        return level.state().definition().id();
    }

    void dispose() {
        if (!timeAccounted) {
            stats().left(levelId(), levelTime);
            timeAccounted = true;
            app.saveStats();
        }
        level.dispose();
    }
}
