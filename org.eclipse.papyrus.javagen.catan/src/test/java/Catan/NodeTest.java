package Catan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    private Node node;
    private Player alice;
    private Board board;

    @BeforeEach
    void setup() {
        alice = new Player("alice");
        node = new Node(5, Set.of(1), Set.of(6));
        // create simple board: node plus neighbor
        Node neighbor = new Node(6, Set.of(1), Set.of(5));
        board = new Board(List.of(new Tile(1, ResourceType.WOOD, 5, Set.of(5,6))),
                List.of(node, neighbor),
                List.of(new Path(1, 5, 6)));
    }

    @Test
    void constructorRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Node(-1, Set.of(1), Set.of(2)));
        assertThrows(IllegalArgumentException.class, () -> new Node(1, Set.of(), Set.of(2)));
        assertThrows(NullPointerException.class, () -> new Node(1, null, Set.of(2)));
    }

    @Test
    void gettersWork() {
        assertEquals(5, node.getId());
        assertTrue(node.getAdjacentNodeIds().contains(6));
        assertTrue(node.getAdjacentTileIds().contains(1));
        assertFalse(node.isClaimed());
        assertEquals(Integer.valueOf(1), node.getAdjacentTileIds().iterator().next());
    }

    @Test
    void claimAndUpgrade() {
        // before claim nothing owned
        assertFalse(node.canUpgradeToCity(alice));
        node.claim(alice);
        assertTrue(node.isClaimed());
        assertTrue(node.isOwnedBy(alice));
        assertEquals(alice, node.getOwner().get());
        assertThrows(IllegalStateException.class, () -> node.claim(new Player("bob")));

        // after claim we should be able to upgrade
        assertTrue(node.canUpgradeToCity(alice));
        node.upgradeToCity(alice);
        assertEquals(BuildingType.CITY, node.getBuilding().getType());
        // second upgrade should fail
        assertThrows(IllegalStateException.class, () -> node.upgradeToCity(alice));
    }

    @Test
    void settlementRules() {
        // a completely empty node with no roads owned
        assertFalse(node.canBuildSettlement(board, alice));
        // give alice a road on the connecting path
        Path p = board.getPath(1);
        p.claim(alice);
        assertTrue(node.canBuildSettlement(board, alice));
        // distance rule: neighbor claimed
        Node neighbor = board.getNode(6);
        neighbor.claim(new Player("bob"));
        assertFalse(node.canBuildSettlement(board, alice));
    }
}