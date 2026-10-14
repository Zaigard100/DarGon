package com.zaig100.dargon.script;

/**
 * Объект хоста (игры), доступный скрипту: {@code obj.field}, {@code obj.field = x}, {@code
 * obj.method(args)}.
 *
 * <p>Реализация решает, какие поля и методы открыты. Неизвестные имена должны приводить к {@link
 * ScriptException} с понятным текстом — для этого есть методы по умолчанию.
 */
public interface ScriptObject extends Value {

    /** Чтение поля {@code obj.name}. */
    default Value getMember(String name) {
        throw new ScriptException("у объекта " + typeName() + " нет поля '" + name + "'");
    }

    /** Запись поля {@code obj.name = value}. */
    default void setMember(String name, Value value) {
        throw new ScriptException(
                "у объекта " + typeName() + " нет изменяемого поля '" + name + "'");
    }

    /** Вызов метода {@code obj.name(args)}. */
    default Value callMethod(String name, Value[] args) {
        throw new ScriptException("у объекта " + typeName() + " нет метода '" + name + "'");
    }

    @Override
    default String typeName() {
        return "object";
    }

    @Override
    default String display() {
        return "<" + typeName() + ">";
    }
}
