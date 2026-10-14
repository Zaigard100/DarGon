package com.zaig100.dargon.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelDocTest {

    @Test
    void customLegendIsRespectedWhenDrawing() {
        LevelDoc doc = LevelDoc.create("x", 3, 1, null);
        java.util.Map<String, Object> legend = new java.util.LinkedHashMap<>();
        legend.put("~", "wall");
        legend.put("o", "floor");
        doc.root().put("legend", legend);
        doc.root().put("tiles", new java.util.ArrayList<>(List.of("o~o")));
        assertTrue(doc.isFloor(0, 0));
        assertFalse(doc.isFloor(1, 0), "~ — стена по легенде");
        assertTrue(doc.setFloor(1, 0, true));
        assertTrue(doc.setFloor(0, 0, false));
        assertEquals(List.of("~oo"), doc.root().get("tiles"));
        doc.resize(4, 1, 0, 0);
        assertEquals(List.of("~oo~"), doc.root().get("tiles"));
    }

    @Test
    void manualEdgesFollowTheMapAndDisappearWhenAllAuto() {
        LevelDoc doc = LevelDoc.create("x", 3, 2, null);
        doc.setFloor(1, 0, false);
        assertTrue(doc.setEdge(1, 0, '3'));
        assertEquals('3', doc.edgeAt(1, 0));
        assertEquals(List.of(".3.", "..."), doc.root().get("edges"));

        doc.resize(4, 3, 1, 1); // столбец слева и строка сверху
        assertEquals('3', doc.edgeAt(2, 1), "край едет вместе с картой");
        assertEquals(3, ((List<?>) doc.root().get("edges")).size());

        assertTrue(doc.setFloor(2, 1, true), "пол на месте края — край сбрасывается");
        assertEquals('.', doc.edgeAt(2, 1));
        assertFalse(doc.root().containsKey("edges"), "все края «авто» — поля нет");
        assertFalse(doc.setEdge(9, 9, '#'), "вне карты — ничего");
    }
}
