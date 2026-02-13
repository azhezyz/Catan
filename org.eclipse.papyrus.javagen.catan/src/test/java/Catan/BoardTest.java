package Catan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    @Test
    void indexByIdRejectsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Board(List.of(), List.of(new Node(1, Set.of(1), Set.of())), List.of(new Path(1,1,2))));
    }

    @Test
    void getNodePathAndAdjacencies() {
        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Path p = new Path(2, 0, 1);
        Tile t = new Tile(0, ResourceType.WOOD, 5, Set.of(0));
        Board board = new Board(List.of(t), List.of(n0, n1), List.of(p));
        assertEquals(n0, board.getNode(0));
        assertEquals(p, board.getPath(2));
        assertThrows(IllegalArgumentException.class, () -> board.getNode(99));
        assertThrows(IllegalArgumentException.class, () -> board.getPath(99));

        List<Path> adj = board.pathsAdjacentToNode(0);
        assertTrue(adj.contains(p));
        assertEquals(List.of(t), board.tilesForRoll(5));
        assertTrue(board.tilesForRoll(99).isEmpty());
    }

    @Test
    void invalidAdjacencyDetected() {
        Node n0 = new Node(0, Set.of(999), Set.of());
        Tile t = new Tile(0, ResourceType.WOOD, 5, Set.of(0));
        // node refers to nonexistent tile id 999 -> should throw when building board
        assertThrows(IllegalArgumentException.class,
                () -> new Board(List.of(t), List.of(n0), List.of()));
    }
}
