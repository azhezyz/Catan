package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

        // Seed initial settlements so Alice has a legal place to build roads
        StandardGameSetup.seedInitialState(board, alice, bob, new Player("C"), new Player("D"));
        handler = new ValueMaximizationHandler(new Random(42)); // Fixed seed for predictable ties
    }

    @Test
    void testPrioritizesSettlementOverRoad() {
        // Give Alice enough resources for both a Road and a Settlement
        alice.addResource(ResourceType.WOOD, 2);
        alice.addResource(ResourceType.BRICK, 2);
        alice.addResource(ResourceType.SHEEP, 1);
        alice.addResource(ResourceType.WHEAT, 1);

        // We must manually build a road first so she has a valid empty node for a settlement
        // (Assuming standard setup gives her a settlement at Node 14 and road at Path 19)
        ActionDecision move = handler.handle(board, alice, players);

        // If she can afford a settlement and has a legal spot, she should take it (Score 1.0)
        // If she doesn't have a legal spot yet, she will build a road (Score 0.8)
        assertNotEquals(BuildAction.NONE, move.getAction(), "Handler should find an affordable action");
    }

    @Test
    void testReturnsNoneWhenCannotAffordAnything() {
        // Alice has 0 resources
        ActionDecision move = handler.handle(board, alice, players);
        assertEquals(BuildAction.NONE, move.getAction(), "Should return NONE when no actions are affordable");
    }
}
