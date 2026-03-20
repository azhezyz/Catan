package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.Board;
import Catan.BuildAction;
import Catan.OverSevenCardsHandler;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;

class OverSevenCardsHandlerTest {
    private Board board;
    private Player alice;
    private List<Player> players;
    private OverSevenCardsHandler handler;

    @BeforeEach
    void setUp() {
        board = StandardGameSetup.buildFullBoard();
        alice = new Player("Alice");
        players = List.of(alice);
        StandardGameSetup.seedInitialState(board, alice, new Player("B"), new Player("C"), new Player("D"));
        handler = new OverSevenCardsHandler();
    }

    @Test
    void testTriggersWhenOverSevenCards() {
        // Give Alice 8 cards (4 Wood, 4 Brick) so she can afford a road
        alice.addResource(ResourceType.WOOD, 4);
        alice.addResource(ResourceType.BRICK, 4);

        ActionDecision decision = handler.handle(board, alice, players);

        // The handler MUST return a build action to drop cards
        assertNotEquals(BuildAction.NONE, decision.getAction(), "Handler should force a purchase when > 7 cards");
    }

    @Test
    void testPassesWhenSevenOrFewerCards() {
        // Give Alice exactly 7 cards
        alice.addResource(ResourceType.WOOD, 4);
        alice.addResource(ResourceType.BRICK, 3);

        ActionDecision decision = handler.handle(board, alice, players);

        // The handler should ignore this and return NONE (passing it down the chain)
        assertEquals(BuildAction.NONE, decision.getAction(), "Handler should return NONE if cards <= 7");
    }
}
