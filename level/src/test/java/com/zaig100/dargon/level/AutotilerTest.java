package com.zaig100.dargon.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AutotilerTest {

    /** Край центральной клетки 3×3, где пол задан символами '.' вокруг. */
    private static WallEdge center(String... rows) {
        return Autotiler.edge(TileMap.parse(rows), 1, 1);
    }

    @Test
    void twoSidedCorners() {
        assertEquals(WallEdge.UP_RIGHT, center("#.#", "##.", "###"));
        assertEquals(WallEdge.DOWN_RIGHT, center("###", "##.", "#.#"));
        assertEquals(WallEdge.LEFT_UP, center("#.#", ".##", "###"));
        assertEquals(WallEdge.LEFT_DOWN, center("###", ".##", "#.#"));
    }

    @Test
    void threeAndFourSides() {
        assertEquals(WallEdge.LEFT_UP_RIGHT, center("#.#", ".#.", "###"));
        assertEquals(WallEdge.LEFT_DOWN_RIGHT, center("###", ".#.", "#.#"));
        assertEquals(WallEdge.UP_DOWN_RIGHT, center("#.#", "##.", "#.#"));
        assertEquals(WallEdge.LEFT_UP_DOWN, center("#.#", ".##", "#.#"));
        // Как в DarGon3: при четырёх соседях побеждает последнее правило — «верх, низ, право».
        assertEquals(WallEdge.UP_DOWN_RIGHT, center("#.#", ".#.", "#.#"));
    }

    @Test
    void noEdge() {
        assertEquals(WallEdge.NONE, center("###", "###", "###"));
        assertEquals(WallEdge.NONE, center("#.#", "###", "###"));
        assertEquals(WallEdge.NONE, center("#.#", "###", "#.#"));
        assertEquals(WallEdge.NONE, center("###", ".#.", "###"));
        assertEquals(WallEdge.NONE, center("...", "...", "..."));
        // Диагональные соседи не учитываются.
        assertEquals(WallEdge.NONE, center(".#.", "###", ".#."));
    }

    @Test
    void mapBorderDoesNotWrapToNeighbourRow() {
        // В DarGon3 левый сосед клетки (0, 1) брался из конца строки 0 — здесь это был бы пол.
        TileMap map = TileMap.parse("##.", "#..", "###");
        assertEquals(WallEdge.NONE, Autotiler.edge(map, 0, 1));
        assertEquals(WallEdge.DOWN_RIGHT, Autotiler.edge(map, 1, 0));
        assertEquals(WallEdge.NONE, Autotiler.edge(TileMap.parse("#"), 0, 0));
    }

    @Test
    void computeCoversWholeMap() {
        TileMap map = TileMap.parse("#.", "..");
        WallEdge[] edges = Autotiler.compute(map);
        assertEquals(4, edges.length);
        assertEquals(WallEdge.DOWN_RIGHT, edges[0]);
        assertEquals(WallEdge.NONE, edges[1]);
    }

    @Test
    void manualEdgesOverrideWallsOnly() {
        TileMap map = TileMap.parse("#.", "..");
        WallEdge[] overrides = {WallEdge.LEFT_UP_DOWN, WallEdge.UP_RIGHT, null, null};
        WallEdge[] edges = Autotiler.compute(map, overrides);
        assertEquals(WallEdge.LEFT_UP_DOWN, edges[0]);
        // На полу край не рисуется даже при правке.
        assertEquals(WallEdge.NONE, edges[1]);
    }

    @Test
    void edgeSymbolsRoundTrip() {
        for (WallEdge edge : WallEdge.values()) {
            assertEquals(edge, WallEdge.bySymbol(edge.symbol()));
        }
        assertEquals('#', WallEdge.NONE.symbol());
        assertEquals('1', WallEdge.LEFT_DOWN.symbol());
        assertNull(WallEdge.bySymbol('.'));
    }
}
