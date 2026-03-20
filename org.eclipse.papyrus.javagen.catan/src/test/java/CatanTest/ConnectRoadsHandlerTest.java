package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.Board;
import Catan.BuildAction;
import Catan.ConnectRoadsHandler;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;

class ConnectRoadsHandlerTest {
    private Board board;
    private Player alice;
    private List<Player> players;
    private ConnectRoadsHandler handler;

    @BeforeEach
    void setUp() {
        board = StandardGameSetup.buildFullBoard();
        alice = new Player("Alice");
        players = List.of(alice);
        handler = new ConnectRoadsHandler();
    }

    @Test
    void testReturnsNoneIfNoConnectionsAvailable() {
        // Alice has resources but no disjointed roads that are 1 gap apart
        StandardGameSetup.seedInitialState(board, alice, new Player("B"), new Player("C"), new Player("D"));
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);

        ActionDecision decision = handler.handle(board, alice, players);

        // Should return NONE because standard setup puts the two starting networks far away from each other
        assertEquals(BuildAction.NONE, decision.getAction(), "Should return NONE if no 1-gap connections exist");
    }
}
