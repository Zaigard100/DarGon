package com.zaig100.dargon.script;

/**
 * Значение DGScript.
 *
 * <p>Встроенные типы: {@link NullValue null}, {@link BoolValue bool}, {@link NumberValue number},
 * {@link StringValue string}, {@link ArrayValue array}, {@link MapValue map}, {@link FunctionValue
 * function}. Объекты игры передаются в скрипт как {@link ScriptObject}. Создавать значения удобно
 * через {@link Values}.
 *
 * <p>Ложными считаются только {@code null}, {@code false} и {@code 0}.
 */
public interface Value {

    /** Имя типа, как его возвращает функция {@code type(x)}: "number", "string", "map"... */
    String typeName();

    /** Истинность в условиях. */
    default boolean isTruthy() {
        return true;
    }

    /** Текстовое представление для {@code print} и {@code str}. */
    String display();
}
