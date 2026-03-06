package CatanTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.*;
import java.util.List;
import java.util.Set;

class BoardTest {

    private List<Tile> sampleTiles;
    private List<Node> sampleNodes;
    private List<Path> samplePaths;

    @BeforeEach
    void setup() {
        // Create a minimal valid board: 1 Tile, 3 Nodes, 3 Paths (a triangle)
        sampleNodes = List.of(
            new Node(0, Set.of(0), Set.of()), 
            new Node(1, Set.of(0), Set.of()), 
            new Node(2, Set.of(0), Set.of())
        );
        sampleTiles = List.of(
            new Tile(0, ResourceType.WOOD, 8, Set.of(0, 1, 2))
        );
        samplePaths = List.of(
            new Path(10, 0, 1),
            new Path(11, 1, 2),
            new Path(12, 2, 0)
        );
    }

    @Test
    void testValidBoardIndexing() {
        Board board = new Board(sampleTiles, sampleNodes, samplePaths);
        // Verify that lookup by ID works correctly
        assertEquals(0, board.getTiles().get(0).getId());
        assertEquals(3, board.getNodes().size());
        assertNotNull(board.getPath(11));
    }

    @Test
    void testInvalidAdjacencyThrowsException() {
        // Create a tile that points to a node ID (99) that doesn't exist on the board
        List<Tile> brokenTiles = List.of(new Tile(1, ResourceType.BRICK, 5, Set.of(99)));
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Board(brokenTiles, sampleNodes, samplePaths);
        }, "Constructor should fail if a tile references a missing node ID.");
    }

    @Test
    void testTilesForRoll() {
        Board board = new Board(sampleTiles, sampleNodes, samplePaths);
        
        // Test roll that should match our sample tile (Number Token 8)
        List<Tile> matches = board.tilesForRoll(8);
        assertEquals(1, matches.size());
        assertEquals(ResourceType.WOOD, matches.get(0).getResource());

        // Test roll that matches nothing
        assertTrue(board.tilesForRoll(2).isEmpty());
    }

    @Test
    void testPathAdjacencyLookup() {
        Board board = new Board(sampleTiles, sampleNodes, samplePaths);
        // Node 0 is connected to Path 10 and Path 12
        List<Path> adjacentTo0 = board.pathsAdjacentToNode(0);
        assertEquals(2, adjacentTo0.size(), "Node 0 should have exactly two adjacent paths.");
    }

    @Test
    void testUnknownNodeAndPathLookupThrows() {
        Board board = new Board(sampleTiles, sampleNodes, samplePaths);
        assertThrows(IllegalArgumentException.class, () -> board.getNode(999));
        assertThrows(IllegalArgumentException.class, () -> board.getPath(999));
        assertThrows(IllegalArgumentException.class, () -> board.pathsAdjacentToNode(999));
    }

    @Test
    void testDuplicateIdsThrow() {
        List<Node> duplicateNodes = List.of(
            new Node(1, Set.of(0), Set.of()),
            new Node(1, Set.of(0), Set.of())
        );
        assertThrows(IllegalArgumentException.class, () -> new Board(
            List.of(new Tile(0, ResourceType.WOOD, 8, Set.of(1))),
            duplicateNodes,
            List.of(new Path(0, 1, 2))
        ));

        List<Path> duplicatePaths = List.of(
            new Path(3, 0, 1),
            new Path(3, 1, 2)
        );
        assertThrows(IllegalArgumentException.class, () -> new Board(sampleTiles, sampleNodes, duplicatePaths));
    }

    @Test
    void testNodeToMissingTileThrows() {
        List<Node> brokenNodes = List.of(new Node(0, Set.of(999), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Board(
            List.of(new Tile(0, ResourceType.WOOD, 6, Set.of(0))),
            brokenNodes,
            List.of(new Path(0, 0, 1))
        ));
    }

    @Test
    void testPathToMissingNodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(
            sampleTiles,
            sampleNodes,
            List.of(new Path(99, 0, 999))
        ));
    }
}
