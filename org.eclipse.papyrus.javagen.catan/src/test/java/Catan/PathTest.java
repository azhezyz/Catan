package Catan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    private Player alice;
    private Board board;
    private Node n0, n1;
    private Path path;

    @BeforeEach
    void setup() {
        alice = new Player("alice");
        // create two nodes and a path connecting them
        n0 = new Node(0, Set.of(0), Set.of(1));
        n1 = new Node(1, Set.of(0), Set.of(0));
        path = new Path(10, 0, 1);
        board = new Board(List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1),
                List.of(path));
    }

    @Test
    void constructorValidatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Path(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Path(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Path(1, 0, 0));
    }

    @Test
    void adjacencyChecks() {
        assertTrue(path.isAdjacentToNode(0));
        assertTrue(path.isAdjacentToNode(1));
        assertFalse(path.isAdjacentToNode(2));
    }

    @Test
    void claimAndOwnership() {
        assertFalse(path.isClaimed());
        path.claim(alice);
        assertTrue(path.isClaimed());
        assertTrue(path.isOwnedBy(alice));
        assertFalse(path.isOwnedBy(new Player("bob")));
        assertThrows(IllegalStateException.class, () -> path.claim(new Player("bob")));
    }

    @Test
    void canBuildRoadScenarios() {
        // initially no building or roads owned -> false
        assertFalse(path.canBuildRoad(board, alice));

        // give alice a building on one end
        n0.claim(alice);
        assertTrue(path.canBuildRoad(board, alice));

        // reset path ownership and node for extension-case
        path = new Path(11, 0, 1);
        // create fresh nodes to avoid duplicate-claim
        n0 = new Node(0, Set.of(0), Set.of(1));
        n1 = new Node(1, Set.of(0), Set.of(0));
        board = new Board(List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1),
                List.of(path));
        n0.claim(alice);
        // now another path from same node
        Path other = new Path(12, 0, 2);
        n1 = new Node(1, Set.of(0), Set.of(0));
        board = new Board(List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1, new Node(2, Set.of(0), Set.of(0))),
                List.of(path, other));
        assertTrue(path.canBuildRoad(board, alice));
    }
}