package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Locale;
import java.util.Set;

/** Тип параметра префаба или поля предмета. Определяет, какие значения допустимы в уровне. */
public enum ParamType {
    /** Целое число. */
    INT,
    /** Любое число. */
    NUMBER,
    BOOL,
    STRING,
    /**
     * Текст для игрока: ключ локализации {@code "@level.05.tablet1"}, строка или массив строк
     * (строки без ключа не переводятся — допустимо только для черновиков).
     */
    TEXT,
    /** Одно из четырёх направлений: {@code right}, {@code down}, {@code left}, {@code up}. */
    DIRECTION,
    /** Одно из восьми направлений, включая диагонали ({@code up_left}…). */
    DIRECTION8,
    /** Клетка {@code [x, y]}. */
    POINT,
    /** Цвет {@code "#RRGGBB"}. */
    COLOR,
    /** Идентификатор уровня: {@code "06"}, {@code "shops/shop_05"}. */
    LEVEL,
    /** {@code id} другой сущности этого уровня. */
    ENTITY,
    /** Стопка предметов: {@code {"id": "coins", "count": 15}}. */
    ITEM,
    /** Массив стопок предметов. */
    ITEMS,
    /**
     * Товары магазина: {@code [{"item": {...}, "price": 10, "stock": 2}]} (запас по умолчанию 1).
     */
    OFFERS,
    /** Урон: целое ≥ 0, диапазон {@code [min, max]} или {@code "kill"} — смерть сразу. */
    DAMAGE;

    /** Имя в JSON: {@code "direction8"}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ParamType byId(String id) {
        for (ParamType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /** Префикс ключа локализации в значениях {@link #TEXT}. */
    public static final String TEXT_KEY_PREFIX = "@";

    /**
     * Проверяет значение (не {@code null}). Ограничения {@code min}/{@code max}/{@code values}
     * проверяет {@link ParamSpec}.
     */
    void validate(JsonValue value, String path, Problems problems, ItemCatalog items) {
        switch (this) {
            case INT:
                if (!JsonValues.isInteger(value)) {
                    problems.error(
                            path, "ожидалось целое число, получено " + JsonValues.describe(value));
                }
                break;
            case NUMBER:
                if (!value.isNumber()) {
                    problems.error(path, "ожидалось число, получено " + JsonValues.describe(value));
                }
                break;
            case BOOL:
                if (!value.isBoolean()) {
                    problems.error(
                            path,
                            "ожидалось true или false, получено " + JsonValues.describe(value));
                }
                break;
            case STRING:
            case LEVEL:
            case ENTITY:
                if (!value.isString() || value.asString().isEmpty()) {
                    problems.error(
                            path,
                            "ожидалась непустая строка, получено " + JsonValues.describe(value));
                }
                break;
            case TEXT:
                validateText(value, path, problems);
                break;
            case DIRECTION:
            case DIRECTION8:
                validateDirection(value, path, problems);
                break;
            case POINT:
                validatePoint(value, path, problems);
                break;
            case COLOR:
                if (!value.isString() || !value.asString().matches("#[0-9A-Fa-f]{6}")) {
                    problems.error(
                            path,
                            "ожидался цвет \"#RRGGBB\", получено " + JsonValues.describe(value));
                }
                break;
            case ITEM:
                items.validateStack(value, path, problems);
                break;
            case ITEMS:
                if (!value.isArray()) {
                    problems.error(
                            path,
                            "ожидался массив предметов, получено " + JsonValues.describe(value));
                    break;
                }
                int i = 0;
                for (JsonValue stack = value.child; stack != null; stack = stack.next, i++) {
                    items.validateStack(stack, JsonValues.index(path, i), problems);
                }
                break;
            case OFFERS:
                validateOffers(value, path, problems, items);
                break;
            case DAMAGE:
                validateDamage(value, path, problems);
                break;
            default:
                throw new IllegalStateException(name());
        }
    }

    private static void validateDamage(JsonValue value, String path, Problems problems) {
        if (value.isString() && value.asString().equals("kill")) {
            return;
        }
        if (JsonValues.isInteger(value) && value.asLong() >= 0) {
            return;
        }
        if (value.isArray()
                && value.size == 2
                && JsonValues.isInteger(value.get(0))
                && JsonValues.isInteger(value.get(1))
                && value.getLong(0) >= 0
                && value.getLong(0) <= value.getLong(1)) {
            return;
        }
        problems.error(
                path,
                "ожидался урон: целое ≥ 0, [min, max] или \"kill\", получено "
                        + JsonValues.describe(value));
    }

    private static void validateText(JsonValue value, String path, Problems problems) {
        if (value.isString()) {
            if (value.asString().equals(TEXT_KEY_PREFIX)) {
                problems.error(path, "пустой ключ локализации");
            }
            return;
        }
        if (value.isArray()) {
            int i = 0;
            for (JsonValue line = value.child; line != null; line = line.next, i++) {
                if (!line.isString()) {
                    problems.error(
                            JsonValues.index(path, i),
                            "ожидалась строка, получено " + JsonValues.describe(line));
                }
            }
            return;
        }
        problems.error(
                path,
                "ожидался ключ \"@...\", строка или массив строк, получено "
                        + JsonValues.describe(value));
    }

    private void validateDirection(JsonValue value, String path, Problems problems) {
        Direction direction = value.isString() ? Direction.byId(value.asString()) : null;
        if (direction == null || (this == DIRECTION && !direction.isOrthogonal())) {
            String allowed =
                    this == DIRECTION
                            ? "right, down, left, up"
                            : "right, down_right, down, down_left, left, up_left, up, up_right";
            problems.error(
                    path,
                    "ожидалось направление ("
                            + allowed
                            + "), получено "
                            + JsonValues.describe(value));
        }
    }

    private static void validatePoint(JsonValue value, String path, Problems problems) {
        if (!value.isArray()
                || value.size != 2
                || !JsonValues.isInteger(value.child)
                || !JsonValues.isInteger(value.child.next)) {
            problems.error(
                    path,
                    "ожидалась клетка [x, y] из двух целых чисел, получено "
                            + JsonValues.describe(value));
        }
    }

    private static void validateOffers(
            JsonValue value, String path, Problems problems, ItemCatalog items) {
        if (!value.isArray()) {
            problems.error(path, "ожидался массив товаров, получено " + JsonValues.describe(value));
            return;
        }
        int i = 0;
        for (JsonValue offer = value.child; offer != null; offer = offer.next, i++) {
            String offerPath = JsonValues.index(path, i);
            if (!offer.isObject()) {
                problems.error(
                        offerPath,
                        "ожидался объект {\"item\": ..., \"price\": ...}, получено "
                                + JsonValues.describe(offer));
                continue;
            }
            JsonValues.checkKeys(offer, offerPath, Set.of("item", "price", "stock"), problems);
            JsonValue item = JsonValues.get(offer, "item");
            if (item == null) {
                problems.error(offerPath, "нет поля 'item'");
            } else {
                items.validateStack(item, JsonValues.child(offerPath, "item"), problems);
            }
            JsonValue price = JsonValues.get(offer, "price");
            if (price == null) {
                problems.error(offerPath, "нет поля 'price'");
            } else if (!JsonValues.isInteger(price) || price.asLong() < 0) {
                problems.error(
                        JsonValues.child(offerPath, "price"),
                        "цена должна быть неотрицательным целым числом, получено "
                                + JsonValues.describe(price));
            }
            JsonValue stock = JsonValues.get(offer, "stock");
            if (stock != null && (!JsonValues.isInteger(stock) || stock.asLong() < 1)) {
                problems.error(
                        JsonValues.child(offerPath, "stock"),
                        "запас должен быть целым числом от 1, получено "
                                + JsonValues.describe(stock));
            }
        }
    }
}
