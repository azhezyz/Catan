package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;

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

        // FIX: Drain the free starting resources
        for (ResourceType type : ResourceType.values()) {
            int count = alice.getResourceCount(type);
            if (count > 0) {
                alice.spend(Map.of(type, count));
            }
        }

        handler = new OverSevenCardsHandler();
    }

    @Test
    void testTriggersWhenOverSevenCards() {
        // Give Alice exactly 8 cards
        alice.addResource(ResourceType.WOOD, 4);
        alice.addResource(ResourceType.BRICK, 4);

        ActionDecision decision = handler.handle(board, alice, players);
        assertNotEquals(BuildAction.NONE, decision.getAction(), "Handler should force a purchase when > 7 cards");
    }

    @Test
    void testPassesWhenSevenOrFewerCards() {
        // Give Alice exactly 7 cards
        alice.addResource(ResourceType.WOOD, 4);
        alice.addResource(ResourceType.BRICK, 3);

        ActionDecision decision = handler.handle(board, alice, players);
        assertEquals(BuildAction.NONE, decision.getAction(), "Handler should return NONE if cards <= 7");
    }
}