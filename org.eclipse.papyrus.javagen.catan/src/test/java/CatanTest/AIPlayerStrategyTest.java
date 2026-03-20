package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Catan.AIPlayerStrategy;
import Catan.ActionDecision;
import Catan.Board;
import Catan.BotRuleHandler;
import Catan.Player;
import Catan.StandardGameSetup;

class AIPlayerStrategyTest {
    private Board board;
    private Player alice;
    private Scanner dummyScanner;

    @BeforeEach
    void setUp() {
        board = StandardGameSetup.buildFullBoard();
        alice = new Player("Alice");
        dummyScanner = new Scanner(System.in);
    }

    @Test
    void testAlwaysRollsFirst() {
        // Create a dummy handler that does nothing
        BotRuleHandler dummyHandler = new BotRuleHandler() {
            @Override
            protected ActionDecision evaluate(Board b, Player p, List<Player> ap) {
                return ActionDecision.none();
            }
        };

        AIPlayerStrategy strategy = new AIPlayerStrategy(dummyHandler);

        String firstCommand = strategy.getNextCommand(board, alice, List.of(alice), dummyScanner);
        assertEquals("roll", firstCommand, "AI must roll the dice on its first action");
    }

    @Test
    void testTranslatesRoadActionCorrectly() {
        // Create a dummy handler that forces a ROAD build at Path ID 1
        BotRuleHandler forceRoadHandler = new BotRuleHandler() {
            @Override
            protected ActionDecision evaluate(Board b, Player p, List<Player> ap) {
                return ActionDecision.road(1); // Force path 1
            }
        };

        AIPlayerStrategy strategy = new AIPlayerStrategy(forceRoadHandler);

        // Burn the roll command
        strategy.getNextCommand(board, alice, List.of(alice), dummyScanner);

        // Get the build command
        String secondCommand = strategy.getNextCommand(board, alice, List.of(alice), dummyScanner);

        // Verify it formats the path ID into the two node IDs with a comma
        assertTrue(secondCommand.startsWith("build road"), "Command should start with 'build road'");
        assertTrue(secondCommand.contains(","), "Command should contain a comma separating node IDs");
    }
}
