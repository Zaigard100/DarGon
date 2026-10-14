package com.zaig100.dargon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.game.GameLog;
import com.zaig100.dargon.i18n.Localization;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.pack.DirectoryPack;
import com.zaig100.dargon.pack.PackLoader;
import com.zaig100.dargon.pack.PackRepository;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.render.LevelRenderer;
import com.zaig100.dargon.screen.App;
import com.zaig100.dargon.settings.Settings;
import com.zaig100.dargon.settings.SettingsStore;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

/**
 * Точка входа игры, общая для всех платформ: загружает ассеты, данные и настройки, собирает {@link
 * App} и открывает главное меню (или сразу уровень — для разработки).
 */
public final class DarGonGame extends ApplicationAdapter {

    /** Логическое разрешение игрового окна: 7×5 тайлов по 16 пикселей. */
    public static final int VIEW_WIDTH = LevelRenderer.VIEW_WIDTH;

    public static final int VIEW_HEIGHT = LevelRenderer.VIEW_HEIGHT;

    private static final int NEW_YEAR_LAST_DAY = 7;
    private static final String PREFERENCES = "dargon-settings";

    private static final GameLog LOG =
            new GameLog() {
                @Override
                public void info(String message) {
                    Gdx.app.log("DarGon", message);
                }

                @Override
                public void error(String message) {
                    Gdx.app.error("DarGon", message);
                }
            };

    private final Platform platform;
    private final String startLevel;
    private final String devPack;
    private PackRepository packs;
    private GameAssets assets;
    private LoadingView loading;
    private App app;

    public DarGonGame(Platform platform) {
        this(platform, null);
    }

    /**
     * @param startLevel уровень для запуска вместо главного меню (для разработки); {@code null} —
     *     меню
     */
    public DarGonGame(Platform platform, String startLevel) {
        this(platform, startLevel, null);
    }

    /**
     * @param devPack каталог пака для пробного запуска из редактора ({@code null} — пак из
     *     настроек): не устанавливается и не запоминается
     */
    public DarGonGame(Platform platform, String startLevel, String devPack) {
        this.platform = platform;
        this.startLevel = startLevel;
        this.devPack = devPack;
    }

    @Override
    public void create() {
        Gdx.app.log("DarGon", "Start on " + platform.name());
        assets = new GameAssets();
        assets.startLoading();
        loading = new LoadingView();
    }

    /** Ассеты загружены: паки, данные, настройки, тексты и приложение. */
    private void start() {
        loading.dispose();
        loading = null;
        packs = new PackRepository(Gdx.files.internal("packs"), platform.packsDir());
        openApp();
        if (app == null) {
            return; // пробный пак не загрузился — игра закрывается
        }
        if (startLevel != null) {
            app.playLevel(startLevel);
        } else {
            app.toMainMenu();
        }
    }

    /**
     * Собирает приложение под пак из настроек. Если он не загрузился — встроенный, а причина
     * показывается в меню.
     */
    private void openApp() {
        Settings settings = new SettingsStore(Gdx.app.getPreferences(PREFERENCES)).load();
        Localization texts = new Localization(Gdx.files::internal, Locale.getDefault());
        AssetReader engine =
                path -> {
                    FileHandle file = Gdx.files.internal(path);
                    return file.exists() ? file.readString("UTF-8") : null;
                };
        if (devPack != null) {
            openDevPack(settings, texts, engine);
            return;
        }
        String failure = null;
        PackRepository.Entry entry = packs.find(settings.pack.get());
        GameData data = null;
        if (entry != null) {
            try {
                data = PackLoader.load(engine, entry, BuildInfo.VERSION);
            } catch (PackLoader.PackException e) {
                failure = e.getMessage();
                LOG.error("Пак '" + settings.pack.get() + "' не загрузился: " + failure);
            }
        } else if (!settings.pack.get().equals(GameData.BUILTIN_PACK)) {
            failure = settings.pack.get();
            LOG.error("Пак '" + settings.pack.get() + "' не найден");
        }
        if (data == null) {
            entry = packs.find(GameData.BUILTIN_PACK);
            data = GameData.load(engine, entry.files());
            settings.pack.set(GameData.BUILTIN_PACK);
        }
        texts.setPack(entry.files());
        assets.usePackTextures(PackLoader.textures(entry.files(), data));
        app =
                new App(
                        platform,
                        assets,
                        data,
                        texts,
                        settings,
                        LOG,
                        isNewYear(LocalDate.now()),
                        packs,
                        this::switchPack);
        if (failure != null) {
            app.packFailed();
        }
    }

    /** Пробный запуск пака из каталога (редактор): без сохранений; не загрузился — выход. */
    private void openDevPack(Settings settings, Localization texts, AssetReader engine) {
        DirectoryPack files = new DirectoryPack(Gdx.files.absolute(devPack));
        GameData data;
        try {
            String header = files.read(PackDefinition.FILE);
            if (header == null) {
                throw new PackLoader.PackException("нет " + PackDefinition.FILE, null);
            }
            data =
                    PackLoader.load(
                            engine,
                            new PackRepository.Entry(
                                    files,
                                    PackDefinition.header(header, PackDefinition.FILE),
                                    false,
                                    null),
                            BuildInfo.VERSION);
        } catch (PackLoader.PackException | LevelFormatException e) {
            LOG.error("Пак " + devPack + " не загрузился: " + e.getMessage());
            platform.exit();
            return;
        }
        texts.setPack(files);
        assets.usePackTextures(PackLoader.textures(files, data));
        app =
                new App(
                        platform,
                        assets,
                        data,
                        texts,
                        settings,
                        LOG,
                        isNewYear(LocalDate.now()),
                        packs,
                        id -> LOG.error("Смена пака недоступна при пробном запуске"));
    }

    /** Сменить пак: после текущего кадра приложение собирается заново (ассеты остаются). */
    private void switchPack(String id) {
        Gdx.app.postRunnable(
                () -> {
                    app.settings().pack.set(id);
                    app.dispose();
                    openApp();
                    app.toMainMenu();
                });
    }

    /** Новогодний костюм героя: 31 декабря — 7 января (как в DarGon3). */
    static boolean isNewYear(LocalDate date) {
        return (date.getMonth() == Month.DECEMBER && date.getDayOfMonth() == 31)
                || (date.getMonth() == Month.JANUARY && date.getDayOfMonth() <= NEW_YEAR_LAST_DAY);
    }

    @Override
    public void render() {
        if (loading != null) {
            if (assets.update()) {
                start();
            } else {
                loading.render(assets.progress());
                return;
            }
        }
        if (app != null) {
            app.render(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void resize(int width, int height) {
        if (app != null) {
            app.resize(width, height);
        }
    }

    @Override
    public void pause() {
        if (app != null) {
            app.pause();
        }
    }

    @Override
    public void dispose() {
        if (loading != null) {
            loading.dispose();
        }
        if (app != null) {
            app.dispose();
        }
        assets.dispose();
    }
}
