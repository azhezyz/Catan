package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.Board;
import Catan.BuildAction;
import Catan.DefendLongestRoadHandler;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;

class DefendLongestRoadHandlerTest {
    private Board board;
    private Player alice;
    private Player bob;
    private List<Player> players;
    private DefendLongestRoadHandler handler;

    @BeforeEach
    void setUp() {
        board = StandardGameSetup.buildFullBoard();
        alice = new Player("Alice");
        bob = new Player("Bob");
        players = List.of(alice, bob);
        handler = new DefendLongestRoadHandler();
    }

    @Test
    void testIgnoresIfRoadIsTooShort() {
        // Alice has no roads, Bob has no roads
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);

        ActionDecision decision = handler.handle(board, alice, players);

        // Rule says Alice must have AT LEAST 4 roads to care about defending
        assertEquals(BuildAction.NONE, decision.getAction(), "Should not defend if own road < 4");
    }
}
