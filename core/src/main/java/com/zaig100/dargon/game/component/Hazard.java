package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Опасность на клетке: наносит урон игроку, который на ней стоит. */
public final class Hazard implements Component {

    /** Урон «убивает сразу» ({@code "damage": "kill"} в префабе). */
    public static final int KILL = Integer.MAX_VALUE;

    /** Урон: случайно из {@code damage..damageMax}; {@link #KILL} — смерть сразу. */
    public int damage;

    public int damageMax;

    /** Вид опасности: выбирает картинку смерти ({@code trap}, {@code spikes}, {@code fall}…). */
    public String kind;

    /** Щит снижает урон. */
    public boolean shieldable;

    /** Срабатывает один раз и исчезает. */
    public boolean once;

    /** Действует только когда хрупкая плитка разрушена. */
    public boolean whenBroken;

    @Override
    public void reset() {
        damage = 0;
        damageMax = 0;
        kind = null;
        shieldable = false;
        once = false;
        whenBroken = false;
    }
}
