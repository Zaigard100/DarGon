package com.zaig100.dargon.game;

/** События игрового мира. */
public final class GameEvents {

    private GameEvents() {}

    /**
     * Урон сущности.
     *
     * @param min наименьший урон (или {@link com.zaig100.dargon.game.component.Hazard#KILL})
     * @param max наибольший урон: выпадает случайно из {@code min..max}
     * @param kind вид урона: выбирает картинку смерти
     * @param shieldable щит принимает часть урона
     * @param periodic урон зоны: не даёт неуязвимости и не учитывает её
     */
    public record Damage(
            int target, int min, int max, String kind, boolean shieldable, boolean periodic) {

        /** Удар с разбросом урона {@code min..max}. */
        public static Damage hit(int target, int min, int max, String kind, boolean shieldable) {
            return new Damage(target, min, max, kind, shieldable, false);
        }

        /** Смерть сразу (пролом). */
        public static Damage kill(int target, String kind) {
            return new Damage(
                    target,
                    com.zaig100.dargon.game.component.Hazard.KILL,
                    com.zaig100.dargon.game.component.Hazard.KILL,
                    kind,
                    false,
                    false);
        }

        /** Урон зоны: 1 HP за период, без неуязвимости и без щита. */
        public static Damage periodic(int target, int amount, String kind) {
            return new Damage(target, amount, amount, kind, false, true);
        }
    }

    /** Игрок нажал «использовать». */
    public record Use(int player) {}

    /** Скрипт уровня выдал достижение: {@code achieve('id')}. */
    public record Achieve(String id) {}

    /** Пасхальное яйцо ({@code "jokkeer_lovushcera"} — выход из игры, куб и т. п.). */
    public record EasterEgg(String code) {}

    /** Игрок шагнул в клетку ({@code x}, {@code y}): клетка уже новая, картинка догоняет. */
    public record Step(int player, int x, int y) {}

    /** Игрок получил урон (после щита): для звука и вибрации. */
    public record Hurt(int player, int damage, boolean died) {}
}
