package com.zaig100.dargon.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.BuildInfo;
import com.zaig100.dargon.Platform;
import com.zaig100.dargon.audio.GdxTrack;
import com.zaig100.dargon.audio.MusicPlayer;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.GameLog;
import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.GameSession;
import com.zaig100.dargon.game.PlayStats;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.game.achievement.AchievementTracker;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.input.GamepadInput;
import com.zaig100.dargon.input.InputMapper;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.pack.PackRepository;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.render.GameTheme;
import com.zaig100.dargon.render.LevelRenderer;
import com.zaig100.dargon.render.PixelScreen;
import com.zaig100.dargon.render.Shaders;
import com.zaig100.dargon.save.AchievementStore;
import com.zaig100.dargon.save.FileStorage;
import com.zaig100.dargon.save.SaveSlots;
import com.zaig100.dargon.save.StatsStore;
import com.zaig100.dargon.settings.Settings;
import com.zaig100.dargon.ui.signal.ReadSignal;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Собранное приложение: сервисы (ассеты, данные, тексты, настройки, ввод), стек экранов и идущая
 * игра. Единственное место, где всё связывается; экраны получают его как контекст.
 */
public final class App {

    private final Platform platform;
    private final GameAssets assets;
    private final GameData data;
    private final Localization texts;
    private final Settings settings;
    private final GameLog log;

    /** Последние сообщения журнала (для экрана разработчика). */
    private final ArrayDeque<String> journal = new ArrayDeque<>();

    private static final int JOURNAL_SIZE = 60;
    private final DevTools dev = new DevTools(this);
    private final boolean newYear;
    private final ActionState input = new ActionState();
    private final InputMapper mapper = new InputMapper(input);
    private final SpriteBatch batch = new SpriteBatch();
    private final PixelScreen pixels =
            new PixelScreen(LevelRenderer.VIEW_WIDTH, LevelRenderer.VIEW_HEIGHT);
    private final Screens screens;
    private final SaveSlots slots;
    private final AchievementStore achievementStore;
    private final AchievementTracker achievements;

    /** Всплывающее сообщение (достижение) и сколько ему ещё висеть. */
    private final Signal<String> toast = Signal.of("");

    private float toastTime;
    private static final float TOAST_SECONDS = 3.5f;
    private final StatsStore statsStore;
    private final PlayStats stats;
    private GameTheme theme;
    private Gameplay gameplay;

    /** Музыка: трек меню и случайные треки уровней (авторы — music/CREDITS.txt). */
    private final MusicPlayer music =
            new MusicPlayer(
                    GdxTrack::new,
                    "menu",
                    List.of("level_dungeon", "level_cavern", "level_drips"),
                    new Random());

    private final PackRepository packs;
    private final Consumer<String> openPack;
    private final GamepadInput gamepad = new GamepadInput(mapper);

    /** Уровень под меню, пока игра не идёт; создаётся при первом показе меню. */
    private MenuBackground menuBackground;

    private boolean debug;
    private ShaderProgram effect;

    public App(
            Platform platform,
            GameAssets assets,
            GameData data,
            Localization texts,
            Settings settings,
            GameLog log,
            boolean newYear,
            PackRepository packs,
            Consumer<String> openPack) {
        this.platform = platform;
        this.packs = packs;
        this.openPack = openPack;
        this.assets = assets;
        this.data = data;
        this.texts = texts;
        this.settings = settings;
        this.log =
                new GameLog() {
                    @Override
                    public void info(String message) {
                        remember(message);
                        log.info(message);
                    }

                    @Override
                    public void error(String message) {
                        remember("! " + message);
                        log.error(message);
                    }
                };
        this.newYear = newYear;
        this.screens = new Screens(this, batch);
        // Встроенный пак — на старом месте (сейвы до паков), остальные — каждый в своей папке.
        String pack = data.pack().id();
        FileStorage storage =
                new FileStorage(
                        pack.equals(GameData.BUILTIN_PACK)
                                ? platform.dataDir()
                                : platform.dataDir().child("packdata").child(pack));
        this.slots = new SaveSlots(storage, data);
        this.statsStore = new StatsStore(storage);
        this.stats = statsStore.load();
        this.achievementStore = new AchievementStore(storage, data.pack().id());
        this.achievements =
                new AchievementTracker(
                        data.pack(), achievementStore, this::achieved, System::currentTimeMillis);
        settings.keys.observe(mapper::setBindings);
        settings.language.observe(
                language ->
                        texts.setLocale(
                                language.isEmpty()
                                        ? Locale.getDefault()
                                        : Locale.forLanguageTag(language)));
        settings.screenEffect.observe(this::applyEffect);
        mapper.setActionListener(this::onAction);
        mapper.setPointerHandler(this::click);
        Gdx.input.setInputProcessor(new InputMultiplexer(screens.stage(), mapper));
        Controllers.addListener(gamepad);
        // Системная кнопка «Назад» на Android — пауза / закрыть экран, а не выход из игры.
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // ------------------------------------------------------------------ сервисы

    public GameAssets assets() {
        return assets;
    }

    public GameData data() {
        return data;
    }

    public Localization texts() {
        return texts;
    }

    public Settings settings() {
        return settings;
    }

    public Screens screens() {
        return screens;
    }

    public Platform platform() {
        return platform;
    }

    public InputMapper input() {
        return mapper;
    }

    /** Идущая игра или {@code null} (главное меню). */
    public Gameplay gameplay() {
        return gameplay;
    }

    /** Версия игры (подставляется при сборке). */
    /** Паки: встроенный и установленные. */
    public PackRepository packs() {
        return packs;
    }

    /** Файлы игры (предметы, префабы) — для проверки пака перед выбором. */
    public AssetReader engineFiles() {
        return path -> {
            FileHandle file = Gdx.files.internal(path);
            return file.exists() ? file.readString("UTF-8") : null;
        };
    }

    /** Переключиться на пак: приложение соберётся заново с его данными. */
    public void openPack(String id) {
        stopGame();
        openPack.accept(id);
    }

    /** Выбранный пак не загрузился — открыт встроенный; сказать об этом в меню. */
    public void packFailed() {
        toast.set(texts.ui("packs.fallback"));
        toastTime = TOAST_SECONDS * 2;
    }

    public String version() {
        return BuildInfo.VERSION;
    }

    public GameLog log() {
        return log;
    }

    DevTools dev() {
        return dev;
    }

    /** Журнал: последние сообщения, новые — в конце. */
    List<String> journal() {
        return new ArrayList<>(journal);
    }

    private void remember(String message) {
        journal.addLast(message);
        while (journal.size() > JOURNAL_SIZE) {
            journal.removeFirst();
        }
    }

    public boolean debug() {
        return debug;
    }

    public void toggleDebug() {
        debug = !debug;
    }

    /** Пост-эффект экрана из настроек (шейдер грузится один раз). */
    private void applyEffect(String name) {
        if (effect != null) {
            effect.dispose();
            effect = null;
        }
        if (name.equals("crt") || name.equals("bloom")) {
            effect = Shaders.load("shaders/" + name + ".frag");
        }
        pixels.setEffect(effect);
    }

    /** Вибрация, если включена в настройках (на устройствах без вибромотора — ничего). */
    public void vibrate(int milliseconds) {
        if (settings.vibration.get()) {
            platform.vibrate(milliseconds);
        }
    }

    /** Показывать ли сенсорные кнопки (меняется в настройках сразу). */
    public ReadSignal<Boolean> touchControls() {
        return settings.touchControls.map(
                mode -> mode.equals("on") || (mode.equals("auto") && platform.isTouchPrimary()));
    }

    /** Звук с громкостью из настроек. */
    public void sound(String name) {
        float volume = settings.soundVolume.get() / (float) Settings.MAX_VOLUME;
        if (volume > 0) {
            assets.play(name, volume);
        }
    }

    // ------------------------------------------------------------------ переходы

    public SaveSlots slots() {
        return slots;
    }

    /** Трекер достижений (события игры). */
    public AchievementTracker achievements() {
        return achievements;
    }

    public AchievementStore achievementStore() {
        return achievementStore;
    }

    /** Всплывающее сообщение для HUD. */
    public ReadSignal<String> toast() {
        return toast;
    }

    private void achieved(PackDefinition.Achievement a) {
        toast.set(texts.ui("achievement.toast", texts.level(a.name())));
        toastTime = TOAST_SECONDS;
        sound("click2");
    }

    /** Стартовый набор режима из пака плюс предметы-награды достижений. */
    public PlayerState startKit(GameMode mode) {
        List<JsonValue> kit = new ArrayList<>(data.pack().startKit(mode.id()));
        for (PackDefinition.Achievement a : data.pack().achievements()) {
            PackDefinition.Reward r = a.reward();
            if (r != null
                    && r.startItem() != null
                    && achievementStore.has(a.id())
                    && (r.modes().isEmpty() || r.modes().contains(mode.id()))
                    && mode != GameMode.CASUAL) {
                kit.add(r.startItem());
            }
        }
        return PlayerState.fromKit(data.items(), kit);
    }

    /** Уровни, с которых открыт старт новой игры (кроме первого). */
    public List<String> unlockedStarts() {
        List<String> starts = new ArrayList<>();
        for (PackDefinition.Achievement a : data.pack().achievements()) {
            if (a.reward() != null
                    && a.reward().unlockStart() != null
                    && achievementStore.has(a.id())) {
                starts.add(a.reward().unlockStart());
            }
        }
        return starts;
    }

    /** Стереть всё: прохождения, достижения, статистику. Настройки остаются. */
    public void resetAll() {
        stopGame();
        for (int i = 0; i < SaveSlots.COUNT; i++) {
            slots.delete(i);
        }
        achievementStore.clear();
        stats.clear();
        statsStore.save(stats);
        toMainMenu();
    }

    public PlayStats stats() {
        return stats;
    }

    public void saveStats() {
        statsStore.save(stats);
    }

    /** Главное меню (прохождение сохраняется). */
    public void toMainMenu() {
        stopGame();
        screens.replaceAll(new MainMenuScreen());
    }

    /** Новая игра в слоте {@code slot} с первого уровня пака. */
    public void newGame(int slot, GameMode mode) {
        newGame(slot, mode, data.pack().start());
    }

    /** Новая игра в слоте {@code slot} с уровня {@code start} (первый или открытый отрезок). */
    public void newGame(int slot, GameMode mode, String start) {
        GameSession session = startSession(slot);
        gameplay.start(session.newGame(mode, start, startKit(mode)));
        showHud();
    }

    /** Продолжить прохождение из слота. */
    public void load(int slot) {
        SaveSlots.Slot saved = slots.slot(slot);
        if (saved.progress() == null) {
            return;
        }
        GameSession session = startSession(slot);
        gameplay.start(session.resume(saved.progress()));
        showHud();
    }

    /** Сразу с уровня (для разработки), без сохранения; прохождение помечается. */
    public void playLevel(String level) {
        GameSession session = startSession(-1);
        gameplay.start(session.enter(level, PlayerState.newGame(data.items())));
        session.markCheats();
        showHud();
    }

    private GameSession startSession(int slot) {
        stopGame();
        GameSession.Saver saver =
                slot < 0
                        ? GameSession.Saver.NONE
                        : progress -> slots.save(slot, progress, System.currentTimeMillis());
        GameSession session = new GameSession(data, input, log, saver, System::currentTimeMillis);
        gameplay =
                new Gameplay(
                        this, session, input, new LevelRenderer(assets, data.items(), newYear));
        return session;
    }

    /** После смерти: уровень заново или контрольная точка. */
    public void afterDeath() {
        gameplay.afterDeath();
        showHud();
    }

    /** Перезапустить текущий уровень (пауза, экран смерти). */
    public void restartLevel() {
        gameplay.restartLevel();
        showHud();
    }

    /** Закрыть всё поверх игры (оставить HUD). */
    public void resume() {
        showHud();
    }

    /** Экраны идущей игры: HUD и сенсорные кнопки (видны, если включены в настройках). */
    void showHud() {
        screens.replaceAll(new HudScreen());
        screens.push(new TouchScreen());
    }

    public void exit() {
        platform.exit();
    }

    void easterEgg(GameEvents.EasterEgg egg) {
        if (egg.code().equals("jokkeer_lovushcera")) {
            exit();
        } else if (egg.code().equals("4601025111298")) {
            screens.push(new CubeScreen());
        } else {
            log.info("Пасхалка '" + egg.code() + "' появится позже");
        }
    }

    private void stopGame() {
        if (menuBackground != null) {
            menuBackground.dispose();
            menuBackground = null;
        }
        if (gameplay != null) {
            gameplay.session().save();
            gameplay.dispose();
            gameplay = null;
        }
        input.clear();
    }

    // ------------------------------------------------------------------ ввод

    private boolean onAction(Action action) {
        if (action == Action.DEBUG && gameplay == null) {
            toggleDebug();
            return true;
        }
        if (screens.onAction(action)) {
            return true;
        }
        return false;
    }

    private void click(int screenX, int screenY) {
        if (gameplay == null || screens.modalTop() != null) {
            return;
        }
        float x = pixels.toLogicalX(screenX);
        float y = pixels.toLogicalYDown(screenY);
        if (pixels.contains(x, y)) {
            gameplay.click(x, y);
        }
    }

    // ------------------------------------------------------------------ цикл

    public void render(float delta) {
        if (toastTime > 0) {
            toastTime -= delta;
            if (toastTime <= 0) {
                toast.set("");
            }
        }
        boolean paused = screens.gamePaused();
        if (gameplay != null) {
            gameplay.update(delta, paused);
        }
        if (gameplay != null) {
            gameplay.draw(batch, pixels, paused);
        } else {
            if (menuBackground == null) {
                menuBackground = new MenuBackground(data, assets, newYear);
            }
            menuBackground.update(delta);
            menuBackground.draw(batch, pixels);
        }
        music.update(
                delta,
                gameplay != null ? MusicPlayer.Mood.GAME : MusicPlayer.Mood.MENU,
                settings.musicVolume.get() / (float) Settings.MAX_VOLUME);
        screens.update(delta);
        screens.draw();
    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean scaleChanged = pixels.resize(width, height);
        if (theme == null || scaleChanged) {
            assets.clearFonts();
            if (theme != null) {
                theme.dispose();
            }
            theme =
                    new GameTheme(
                            assets, pixels.scale(), () -> sound("click2"), () -> sound("click1"));
        }
        screens.resize(width, height, pixels.scale(), pixels.originX(), pixels.originY(), theme);
    }

    public void pause() {
        if (gameplay != null) {
            // Приложение свернули (на Android могут и закрыть): сохраняем время игры.
            gameplay.session().save();
            if (!screens.gamePaused()) {
                screens.push(new PauseScreen());
            }
        }
    }

    public void dispose() {
        stopGame();
        Controllers.removeListener(gamepad);
        music.dispose();
        screens.dispose();
        if (theme != null) {
            theme.dispose();
        }
        pixels.dispose();
        if (effect != null) {
            effect.dispose();
        }
        batch.dispose();
    }
}
