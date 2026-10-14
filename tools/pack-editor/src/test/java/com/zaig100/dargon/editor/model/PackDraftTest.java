package com.zaig100.dargon.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackDraftTest {

    @TempDir Path dir;

    @Test
    void textsAndRenamesApplyOnlyOnApply() throws IOException {
        PackProject project = PackProject.create(dir.resolve("p"), "p", "P");
        PackDraft draft = new PackDraft(project);
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", "first");
        a.put("name", "Старое имя");
        draft.setText(a, "name", "ach.first.name", "ru", "Первый шаг");
        assertEquals("@ach.first.name", a.get("name"));
        assertEquals(
                "Старое имя", draft.text("en", "ach.first.name"), "обычная строка стала ключом");
        assertEquals("Первый шаг", draft.resolve(a.get("name"), "ru"));
        assertNull(project.text("ru", "ach.first.name"), "до «Применить» проект не меняется");

        draft.moveText("ach.first.name", "ach.step.name");
        assertEquals("Первый шаг", draft.text("ru", "ach.step.name"));
        assertFalse(draft.hasText("ach.first.name"));

        draft.pack().put("achievements", List.of(a));
        draft.apply();
        assertEquals("Первый шаг", project.text("ru", "ach.step.name"));
        assertNull(project.text("ru", "ach.first.name"));
        assertEquals(List.of(a), project.pack().get("achievements"));

        PackDraft cancelled = new PackDraft(project);
        cancelled.pack().put("version", "9.9");
        cancelled.text("ru", "pack.name", "Другое");
        assertEquals("1.0", project.pack().get("version"));
        assertEquals("P", project.text("ru", "pack.name"));
    }
}
