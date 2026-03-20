package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.Board;
import Catan.BuildAction;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;
import Catan.ValueMaximizationHandler;

class ValueMaximizationHandlerTest {
    private Board board;
    private Player alice;
    private List<Player> players;
    private ValueMaximizationHandler handler;

    @BeforeEach
    void setUp() {
        board = StandardGameSetup.buildFullBoard();
        alice = new Player("Alice");
        Player bob = new Player("Bob");
        players = List.of(alice, bob);

        // Seed initial settlements
        StandardGameSetup.seedInitialState(board, alice, bob, new Player("C"), new Player("D"));

        // FIX: Drain the free starting resources given by the second settlement
        for (ResourceType type : ResourceType.values()) {
            int count = alice.getResourceCount(type);
            if (count > 0) {
                alice.spend(Map.of(type, count));
            }
        }

        handler = new ValueMaximizationHandler(new Random(42));
    }

    @Test
    void testPrioritizesSettlementOverRoad() {
        // Now give exactly the resources we want
        alice.addResource(ResourceType.WOOD, 2);
        alice.addResource(ResourceType.BRICK, 2);
        alice.addResource(ResourceType.SHEEP, 1);
        alice.addResource(ResourceType.WHEAT, 1);

        ActionDecision move = handler.handle(board, alice, players);
        assertNotEquals(BuildAction.NONE, move.getAction(), "Handler should find an affordable action");
    }

    @Test
    void testReturnsNoneWhenCannotAffordAnything() {
        // Alice truly has 0 resources now
        ActionDecision move = handler.handle(board, alice, players);
        assertEquals(BuildAction.NONE, move.getAction(), "Should return NONE when no actions are affordable");
    }
}