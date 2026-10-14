package com.zaig100.dargon.editor.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Черновик свойств пака: копия {@code pack.json} и правки текстов. Применяется к проекту целиком
 * («Применить») или отбрасывается («Отмена»).
 */
public final class PackDraft {

    private final PackProject project;
    private final Map<String, Object> pack;
    private final Map<String, Map<String, String>> texts = new HashMap<>();
    private final Set<String> removed = new HashSet<>();

    public PackDraft(PackProject project) {
        this.project = project;
        this.pack = Json.copy(project.pack());
        for (String lang : PackProject.LANGUAGES.keySet()) {
            texts.put(lang, new HashMap<>());
        }
    }

    public PackProject project() {
        return project;
    }

    public Map<String, Object> pack() {
        return pack;
    }

    /** Текст по ключу (без {@code @}) с учётом черновика; нет — пустая строка. */
    public String text(String lang, String key) {
        if (removed.contains(key)) {
            return "";
        }
        String draft = texts.get(lang).get(key);
        if (draft != null) {
            return draft;
        }
        String saved = project.text(lang, key);
        return saved == null ? "" : saved;
    }

    public void text(String lang, String key, String value) {
        removed.remove(key);
        texts.get(lang).put(key, value);
    }

    public boolean hasText(String key) {
        if (removed.contains(key)) {
            return false;
        }
        for (String lang : PackProject.LANGUAGES.keySet()) {
            if (texts.get(lang).containsKey(key) || project.text(lang, key) != null) {
                return true;
            }
        }
        return false;
    }

    /** Переносит тексты ключа на новый ключ (переименование достижения). */
    public void moveText(String from, String to) {
        for (String lang : PackProject.LANGUAGES.keySet()) {
            text(lang, to, text(lang, from));
        }
        removed.add(from);
        for (String lang : PackProject.LANGUAGES.keySet()) {
            texts.get(lang).remove(from);
        }
    }

    /** Значение поля-текста: {@code "@ключ"} → текст ключа; обычная строка — как есть. */
    public String resolve(Object value, String lang) {
        if (value instanceof String s && s.startsWith("@")) {
            return text(lang, s.substring(1));
        }
        return value == null ? "" : String.valueOf(value);
    }

    /** Записывает перевод в ключ поля; обычная строка сначала становится ключом {@code key}. */
    public void setText(
            Map<String, Object> owner, String field, String key, String lang, String v) {
        Object value = owner.get(field);
        String target;
        if (value instanceof String s && s.startsWith("@")) {
            target = s.substring(1);
        } else {
            target = key;
            for (String other : PackProject.LANGUAGES.keySet()) {
                text(other, target, resolve(value, other));
            }
            owner.put(field, "@" + target);
        }
        text(lang, target, v);
    }

    /** Переносит черновик в проект. */
    public void apply() {
        for (String key : removed) {
            for (String lang : PackProject.LANGUAGES.keySet()) {
                project.removeText(lang, key);
            }
        }
        for (Map.Entry<String, Map<String, String>> lang : texts.entrySet()) {
            for (Map.Entry<String, String> e : lang.getValue().entrySet()) {
                project.text(lang.getKey(), e.getKey(), e.getValue());
            }
        }
        project.pack().clear();
        project.pack().putAll(pack);
        project.packChanged();
    }

    /** Уровни пака — для выбора в условиях и наградах. */
    public List<String> levels() {
        return project.levelIds();
    }
}
