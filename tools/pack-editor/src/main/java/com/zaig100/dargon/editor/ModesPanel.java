package com.zaig100.dargon.editor;

import com.kotcrab.vis.ui.widget.VisTable;
import com.zaig100.dargon.editor.model.PackDraft;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.PackDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Режимы пака: стартовый набор предметов для казуала, обычного и хардкора. */
final class ModesPanel extends VisTable {

    static final List<String> LABELS =
            List.of(
                    "Казуал — смерть: уровень заново; переход лечит до половины; без достижений",
                    "Обычный — 3 смерти подряд: откат к контрольной точке",
                    "Хардкор — сохранение только на контрольных точках; любая смерть — откат");

    @SuppressWarnings("unchecked")
    ModesPanel(PackDraft draft, ItemCatalog items, Runnable changed) {
        top().left();
        defaults().left().pad(3);
        Map<String, Object> modes =
                (Map<String, Object>)
                        draft.pack().computeIfAbsent("modes", k -> new LinkedHashMap<>());
        for (int i = 0; i < PackDefinition.MODES.size(); i++) {
            String mode = PackDefinition.MODES.get(i);
            Map<String, Object> m =
                    (Map<String, Object>) modes.computeIfAbsent(mode, k -> new LinkedHashMap<>());
            List<Object> start = (List<Object>) m.computeIfAbsent("start", k -> new ArrayList<>());
            add(Form.heading(LABELS.get(i))).padTop(i == 0 ? 0 : 10).row();
            add(new StackEditor.ListEditor(items, start, false, changed)).row();
        }
        add(Form.hint(
                        "Стартовый набор — что лежит в инвентаре в начале новой игры. Достижения"
                                + " могут добавлять к нему предметы (награда «предмет на старте»).",
                        700))
                .width(700)
                .padTop(10)
                .row();
    }
}
