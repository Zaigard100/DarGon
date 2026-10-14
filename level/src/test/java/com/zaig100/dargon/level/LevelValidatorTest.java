package com.zaig100.dargon.level;

import static com.zaig100.dargon.level.TestData.level;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LevelValidatorTest {

    /**
     * Контекст: уровни "next", скрипты "ok.dgs", "bad.dgs" и "uses.dgs", ключи "test" и "only.en"
     * (только en).
     */
    private static final LevelValidator.Context CONTEXT =
            new LevelValidator.Context() {
                @Override
                public boolean levelExists(String id) {
                    return id.equals("next");
                }

                @Override
                public String scriptSource(String path) {
                    return Map.of(
                                    "ok.dgs",
                                    "def f() { entity('b1').disable() }",
                                    "bad.dgs",
                                    "x = (1",
                                    "uses.dgs",
                                    "use 'ok'\nuse \"math\"\nuse 'bad'\nuse 'none'\nuse 'nested'\n"
                                            + "// use 'commented'\nprint(\"use 'in string'\")",
                                    "nested.dgs",
                                    "use 'uses'\nuse 'deep'")
                            .get(path);
                }

                @Override
                public Collection<String> locales() {
                    return List.of("en", "ru");
                }

                @Override
                public boolean hasText(String locale, String key) {
                    return key.equals("test") || (key.equals("only.en") && locale.equals("en"));
                }
            };

    private static List<String> problems(String json) {
        Problems problems = new Problems("test");
        new LevelValidator(CONTEXT).validate(TestData.parse(json), problems);
        return problems.list().stream().map(Problem::toString).collect(Collectors.toList());
    }

    private static String stairs(String at) {
        return "{ \"prefab\": \"stairs\", \"at\": " + at + ", \"next\": \"next\" }";
    }

    @Test
    void cleanLevelHasNoProblems() {
        assertEquals(List.of(), problems(level("[0, 0]", List.of("...", "..."), stairs("[2, 1]"))));
    }

    @Test
    void diagonalStepsAndTeleportsReachExit() {
        // По диагонали: (0,0) → (1,1) → (2,2).
        assertEquals(
                List.of(),
                problems(level("[0, 0]", List.of(".##", "#.#", "##."), stairs("[2, 2]"))));
        // Через телепорт.
        assertEquals(
                List.of(),
                problems(
                        level(
                                "[0, 0]",
                                List.of(".#.", "###"),
                                stairs("[2, 0]"),
                                "{ \"prefab\": \"teleport\", \"at\": [0, 0], \"target\": [2, 0] }")));
    }

    @Test
    void unreachableExitIsWarning() {
        List<String> problems = problems(level("[0, 0]", List.of(".#."), stairs("[2, 0]")));
        assertEquals(
                List.of(
                        "test: entities[0].at: предупреждение: выход [2, 0] недостижим от точки появления без помощи скриптов"),
                problems);
    }

    @Test
    void blockingEntitiesBlockButLockedDoorsDoNot() {
        assertEquals(
                1,
                problems(
                                level(
                                        "[0, 0]",
                                        List.of("..."),
                                        stairs("[2, 0]"),
                                        "{ \"prefab\": \"wall_block\", \"at\": [1, 0] }"))
                        .size());
        assertEquals(
                List.of(),
                problems(
                        level(
                                "[0, 0]",
                                List.of("..."),
                                stairs("[2, 0]"),
                                "{ \"prefab\": \"door\", \"at\": [1, 0], \"key\": \"k\" }")));
    }

    @Test
    void scriptedExitIsFine() {
        assertEquals(
                List.of(),
                problems(
                        level(
                                "[0, 0]",
                                List.of(".#."),
                                "{ \"prefab\": \"stairs\", \"at\": [2, 0], \"enabled\": false }")));
    }

    @Test
    void missingExitIsError() {
        assertTrue(
                problems(level("[0, 0]", List.of(".")))
                        .get(0)
                        .contains("ошибка: в уровне нет выхода"));
    }

    @Test
    void references() {
        List<String> problems =
                problems(
                        level(
                                "[0, 0]",
                                List.of("..."),
                                "{ \"prefab\": \"stairs\", \"at\": [2, 0], \"next\": \"nowhere\" }",
                                "{ \"prefab\": \"teleport\", \"at\": [1, 0], \"target\": [5, 5] }"));
        assertTrue(
                problems.contains("test: entities[0].next: ошибка: уровень 'nowhere' не найден"),
                problems.toString());
        assertTrue(
                problems.contains(
                        "test: entities[1].target: предупреждение: клетка [5, 5] не на полу"),
                problems.toString());
    }

    @Test
    void placement() {
        List<String> problems =
                problems(
                        level(
                                "[0, 0]",
                                List.of("..#"),
                                stairs("[1, 0]"),
                                "{ \"prefab\": \"flamethrower\", \"at\": [0, 0] }",
                                "{ \"prefab\": \"flamethrower\", \"at\": [2, 0] }",
                                "{ \"prefab\": \"flamethrower\", \"at\": [-1, 0] }",
                                "{ \"prefab\": \"button\", \"at\": [2, 0] }",
                                "{ \"prefab\": \"button\", \"at\": [9, 9] }"));
        assertEquals(
                Set.of(
                        "test: entities[1].at: предупреждение: flamethrower стоит на полу [0, 0], обычно его ставят на стену или за край карты",
                        "test: entities[4].at: предупреждение: button стоит на стене [2, 0]",
                        "test: entities[5].at: предупреждение: button за пределами карты [9, 9]"),
                Set.copyOf(problems));
    }

    @Test
    void scriptsAreCompiledAndEntityIdsChecked() {
        List<String> problems =
                problems(
                        level(
                                "[0, 0]",
                                List.of("..."),
                                stairs("[2, 0]"),
                                "{ \"prefab\": \"button\", \"id\": \"b1\", \"at\": [1, 0], \"onUse\": \"entity('b1').disable()\" }",
                                "{ \"prefab\": \"button\", \"at\": [1, 0], \"onUse\": [\"entity(\\\"ghost\\\").enable()\"] }",
                                "{ \"prefab\": \"button\", \"at\": [1, 0], \"onUse\": \"if {\" }"));
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(
                problems.get(0)
                        .startsWith(
                                "test: entities[2].onUse: ошибка: entity(\"ghost\"): в уровне нет сущности с таким id"),
                problems.toString());
        assertTrue(
                problems.get(1)
                        .startsWith(
                                "test: entities[3].onUse: ошибка: test.json:entities[3].onUse:1:"),
                problems.toString());
    }

    @Test
    void levelScriptFile() {
        String base =
                "{ \"formatVersion\": 1, \"name\": \"@test\", \"spawn\": [0, 0], \"tiles\": [\"..\"], \"entities\": [ { \"prefab\": \"stairs\", \"at\": [1, 0], \"next\": \"next\" }, { \"prefab\": \"button\", \"id\": \"b1\", \"at\": [1, 0] } ], \"script\": ";
        assertEquals(List.of(), problems(base + "\"ok.dgs\" }"));
        assertTrue(
                problems(base + "\"bad.dgs\" }")
                        .get(0)
                        .startsWith("test: script: ошибка: bad.dgs:1:"));
        assertEquals(
                List.of("test: script: ошибка: файл скрипта не найден: none.dgs"),
                problems(base + "\"none.dgs\" }"));
        List<String> uses = problems(base + "\"uses.dgs\" }");
        assertEquals(3, uses.size(), uses.toString());
        assertTrue(uses.get(0).startsWith("test: script: ошибка: use \"bad\": bad.dgs:1:"));
        assertEquals("test: script: ошибка: use \"none\": нет файла none.dgs", uses.get(1));
        assertEquals("test: script: ошибка: use \"deep\": нет файла deep.dgs", uses.get(2));
    }

    @Test
    void localization() {
        List<String> problems =
                problems(
                        level(
                                "[0, 0]",
                                List.of(".."),
                                stairs("[1, 0]"),
                                "{ \"prefab\": \"tablet\", \"at\": [0, 0], \"text\": \"@only.en\" }",
                                "{ \"prefab\": \"tablet\", \"at\": [0, 0], \"text\": [\"draft\"] }"));
        assertEquals(
                List.of(
                        "test: entities[1].text: ошибка: нет перевода ключа 'only.en' для языка ru",
                        "test: entities[2].text: предупреждение: текст не локализован: используйте ключ \"@...\""),
                problems);
    }
}
