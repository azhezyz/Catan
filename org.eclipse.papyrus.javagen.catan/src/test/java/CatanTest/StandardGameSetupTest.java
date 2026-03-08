package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Catan.Board;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;
import Catan.Tile;
import Catan.Node;
import Catan.Path;

class StandardGameSetupTest {

    @Test
    void buildFullBoardCreatesExpectedTopology() {
        Board board = StandardGameSetup.buildFullBoard();

        assertEquals(19, board.getTiles().size());
        assertEquals(54, board.getNodes().size());
        assertEquals(72, board.getPaths().size());

        List<Tile> deserts = board.getTiles().stream()
                .filter(tile -> tile.getResourceType() == null)
                .toList();
        assertEquals(1, deserts.size(), "Standard board should contain exactly one desert tile.");
        assertEquals(0, deserts.get(0).getNumberToken(), "Desert tile should have token 0.");
    }

    @Test
    void partitionTesting_tilesForRollInputPartitions() {
        Board board = StandardGameSetup.buildFullBoard();

        // Partition A: legal roll and token exists on board
        List<Tile> hits = board.tilesForRoll(6);
        assertFalse(hits.isEmpty());
        assertTrue(hits.stream().allMatch(tile -> tile.getNumberToken() == 6));

        // Partition B: legal roll but token does not exist on this board
        assertTrue(board.tilesForRoll(7).isEmpty());

        // Partition C: out-of-range roll
        assertTrue(board.tilesForRoll(1).isEmpty());
        assertTrue(board.tilesForRoll(13).isEmpty());
    }

    @Test
    void boundaryTesting_nodeAndPathIdMinMaxAndOutOfRange() {
        Board board = StandardGameSetup.buildFullBoard();

        // Valid boundaries
        assertEquals(0, board.getNode(0).getId());
        assertEquals(53, board.getNode(53).getId());
        assertEquals(0, board.getPath(0).getId());
        assertEquals(71, board.getPath(71).getId());

        // Out-of-range just outside boundaries
        assertThrows(IllegalArgumentException.class, () -> board.getNode(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getNode(54));
        assertThrows(IllegalArgumentException.class, () -> board.getPath(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getPath(72));
    }

    @Test
    void seedInitialStatePlacesPiecesAndGrantsStarterResources() {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("alice");
        Player bob = new Player("bob");
        Player charlie = new Player("charlie");
        Player diana = new Player("diana");

        StandardGameSetup.seedInitialState(board, alice, bob, charlie, diana);

        for (Player player : List.of(alice, bob, charlie, diana)) {
            assertEquals(2, player.getSettlementNodeIds().size(), player.getName() + " should start with two settlements.");
            assertEquals(2, player.getRoadPathIds().size(), player.getName() + " should start with two roads.");
            assertEquals(1, player.getResourceCount(ResourceType.WOOD));
            assertEquals(1, player.getResourceCount(ResourceType.BRICK));
            assertEquals(1, player.getResourceCount(ResourceType.SHEEP));
            assertEquals(1, player.getResourceCount(ResourceType.WHEAT));
            assertEquals(0, player.getResourceCount(ResourceType.ORE));
            assertEquals(2, player.getVictoryPoints(), "Two starting settlements should give 2 VP.");
        }

        assertTrue(board.getNode(18).isOwnedBy(alice));
        assertTrue(board.getNode(13).isOwnedBy(alice));
        assertTrue(board.getNode(11).isOwnedBy(bob));
        assertTrue(board.getNode(7).isOwnedBy(bob));
        assertTrue(board.getNode(19).isOwnedBy(charlie));
        assertTrue(board.getNode(9).isOwnedBy(charlie));
        assertTrue(board.getNode(15).isOwnedBy(diana));
        assertTrue(board.getNode(23).isOwnedBy(diana));
    }

    @Test
    void seedInitialStateTriggersFallbackBFS() {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = StandardGameSetup.class.getDeclaredMethod(
                "claimRoadByNodes", Board.class, Player.class, int.class, int.class);
            method.setAccessible(true);
            method.invoke(null, board, alice, 18, 5);
        });
        
        assertFalse(alice.getRoadPathIds().isEmpty());
    }

    @Test
    void claimRoadByNodesThrowsExceptionForDisconnectedNodes() throws Exception {
        Node n1 = new Node(0, Set.of(0), Set.of());
        Node n2 = new Node(1, Set.of(1), Set.of());
        Tile t1 = new Tile(0, ResourceType.WOOD, 5, Set.of(0));
        Tile t2 = new Tile(1, ResourceType.BRICK, 6, Set.of(1));
        Board brokenBoard = new Board(List.of(t1, t2), List.of(n1, n2), List.of());

        Player alice = new Player("Alice");

        java.lang.reflect.Method method = StandardGameSetup.class.getDeclaredMethod(
            "claimRoadByNodes", Board.class, Player.class, int.class, int.class);
        method.setAccessible(true);

        java.lang.reflect.InvocationTargetException ite = assertThrows(
            java.lang.reflect.InvocationTargetException.class, 
            () -> method.invoke(null, brokenBoard, alice, 0, 1)
        );
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("No road placement found"));
    }

    @Test
    void findFallbackPathReturnsNullForUnreachableNode() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        
        java.lang.reflect.Method method = StandardGameSetup.class.getDeclaredMethod(
            "findFallbackPathId", Board.class, int.class, int.class);
        method.setAccessible(true);

        Object result = method.invoke(null, board, 0, 999);
        assertNull(result);
    }
}
