package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Catan.Board;
import Catan.BuildCityCommand;
import Catan.BuildRoadCommand;
import Catan.BuildSettlementCommand;
import Catan.BuildingType;
import Catan.Node;
import Catan.Path;
import Catan.Player;
import Catan.ResourceType;
import Catan.Tile;

class BuildCommandTest {

    private static Board twoNodeBoard() {
        return new Board(
                List.of(new Tile(0, ResourceType.WOOD, 6, Set.of(0, 1))),
                List.of(
                        new Node(0, Set.of(0), Set.of(1)),
                        new Node(1, Set.of(0), Set.of(0))
                ),
                List.of(new Path(0, 0, 1))
        );
    }

    private static Board threeNodeLineBoard() {
        return new Board(
                List.of(new Tile(0, ResourceType.WOOD, 6, Set.of(0, 1, 2))),
                List.of(
                        new Node(0, Set.of(0), Set.of(1)),
                        new Node(1, Set.of(0), Set.of(0, 2)),
                        new Node(2, Set.of(0), Set.of(1))
                ),
                List.of(
                        new Path(0, 0, 1),
                        new Path(1, 1, 2)
                )
        );
    }

    @Test
    void buildRoadCommandFailsWithIllegalPlacement() {
        Board board = threeNodeLineBoard();
        Player alice = new Player("Alice");
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);

        BuildRoadCommand command = new BuildRoadCommand(board, alice, 1);

        assertFalse(command.execute());
        assertEquals("illegal placement", command.getFailureReason());
    }

    @Test
    void buildRoadCommandFailsWhenResourcesAreInsufficient() {
        Board board = threeNodeLineBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);

        BuildRoadCommand command = new BuildRoadCommand(board, alice, 0);

        assertFalse(command.execute());
        assertEquals("insufficient resources", command.getFailureReason());
    }

    @Test
    void buildRoadCommandExecuteAndUndoRestoreState() {
        Board board = threeNodeLineBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);

        BuildRoadCommand command = new BuildRoadCommand(board, alice, 0);
        assertTrue(command.execute());
        assertTrue(board.getPath(0).isOwnedBy(alice));
        assertTrue(alice.getRoadPathIds().contains(0));
        assertEquals(0, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(0, alice.getResourceCount(ResourceType.BRICK));

        command.undo();
        assertFalse(board.getPath(0).isClaimed());
        assertFalse(alice.getRoadPathIds().contains(0));
        assertEquals(1, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(1, alice.getResourceCount(ResourceType.BRICK));
    }

    @Test
    void buildSettlementCommandFailsWithIllegalPlacement() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);
        alice.addResource(ResourceType.SHEEP, 1);
        alice.addResource(ResourceType.WHEAT, 1);

        BuildSettlementCommand command = new BuildSettlementCommand(board, alice, 1);

        assertFalse(command.execute());
        assertEquals("illegal placement", command.getFailureReason());
    }

    @Test
    void buildSettlementCommandFailsWhenResourcesAreInsufficient() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        board.getPath(0).claim(alice);
        alice.addRoad(0);

        BuildSettlementCommand command = new BuildSettlementCommand(board, alice, 1);

        assertFalse(command.execute());
        assertEquals("insufficient resources", command.getFailureReason());
    }

    @Test
    void buildSettlementCommandExecuteAndUndoRestoreState() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        board.getPath(0).claim(alice);
        alice.addRoad(0);
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);
        alice.addResource(ResourceType.SHEEP, 1);
        alice.addResource(ResourceType.WHEAT, 1);

        BuildSettlementCommand command = new BuildSettlementCommand(board, alice, 1);
        assertTrue(command.execute());
        assertTrue(board.getNode(1).isOwnedBy(alice));
        assertTrue(alice.getSettlementNodeIds().contains(1));
        assertEquals(0, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(0, alice.getResourceCount(ResourceType.BRICK));
        assertEquals(0, alice.getResourceCount(ResourceType.SHEEP));
        assertEquals(0, alice.getResourceCount(ResourceType.WHEAT));

        command.undo();
        assertFalse(board.getNode(1).isClaimed());
        assertFalse(alice.getSettlementNodeIds().contains(1));
        assertEquals(1, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(1, alice.getResourceCount(ResourceType.BRICK));
        assertEquals(1, alice.getResourceCount(ResourceType.SHEEP));
        assertEquals(1, alice.getResourceCount(ResourceType.WHEAT));
    }

    @Test
    void buildCityCommandFailsWithIllegalUpgrade() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        alice.addResource(ResourceType.WHEAT, 2);
        alice.addResource(ResourceType.ORE, 3);

        BuildCityCommand command = new BuildCityCommand(board, alice, 0);

        assertFalse(command.execute());
        assertEquals("illegal upgrade", command.getFailureReason());
    }

    @Test
    void buildCityCommandFailsWhenResourcesAreInsufficient() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);

        BuildCityCommand command = new BuildCityCommand(board, alice, 0);

        assertFalse(command.execute());
        assertEquals("insufficient resources", command.getFailureReason());
    }

    @Test
    void buildCityCommandExecuteAndUndoRestoreState() {
        Board board = twoNodeBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WHEAT, 2);
        alice.addResource(ResourceType.ORE, 3);

        BuildCityCommand command = new BuildCityCommand(board, alice, 0);
        assertTrue(command.execute());
        assertEquals(BuildingType.CITY, board.getNode(0).getBuilding().getType());
        assertTrue(alice.getCityNodeIds().contains(0));
        assertFalse(alice.getSettlementNodeIds().contains(0));
        assertEquals(0, alice.getResourceCount(ResourceType.WHEAT));
        assertEquals(0, alice.getResourceCount(ResourceType.ORE));

        command.undo();
        assertEquals(BuildingType.SETTLEMENT, board.getNode(0).getBuilding().getType());
        assertFalse(alice.getCityNodeIds().contains(0));
        assertTrue(alice.getSettlementNodeIds().contains(0));
        assertEquals(2, alice.getResourceCount(ResourceType.WHEAT));
        assertEquals(3, alice.getResourceCount(ResourceType.ORE));
    }
}

