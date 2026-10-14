package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Map;

/**
 * Тип предмета из {@code assets/data/items.json}.
 *
 * @param id идентификатор: {@code "potion"}
 * @param name название для игрока (ключ локализации)
 * @param icon имя кадра в атласе
 * @param maxStack сколько штук помещается в одну ячейку инвентаря
 * @param effect что происходит при использовании (см. {@link ItemCatalog#EFFECTS})
 * @param args параметры эффекта (объект; может быть пустым)
 * @param fields дополнительные поля стопки в уровне: цвет ключа, сила зелья…
 */
public record ItemDefinition(
        String id,
        String name,
        String icon,
        int maxStack,
        String effect,
        JsonValue args,
        Map<String, ParamSpec> fields) {}
