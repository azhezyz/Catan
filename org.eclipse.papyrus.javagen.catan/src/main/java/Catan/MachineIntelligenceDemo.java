package Catan;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class MachineIntelligenceDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Machine Intelligence Showcase...");

        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        Player charlie = new Player("Charlie");
        Player diana = new Player("Diana");
        List<Player> players = List.of(alice, bob, charlie, diana);

        // 1. Assemble the AI Brain (Chain of Responsibility)
        BotRuleHandler overSeven = new OverSevenCardsHandler();
        BotRuleHandler connectRoads = new ConnectRoadsHandler();
        BotRuleHandler defendRoad = new DefendLongestRoadHandler();
        BotRuleHandler valueMax = new ValueMaximizationHandler(new Random(42)); // Fixed seed for demo

        overSeven.setNext(connectRoads);
        connectRoads.setNext(defendRoad);
        defendRoad.setNext(valueMax);

        // 2. Attach the Strategy to the players
        for (Player p : players) {
            p.setStrategy(new AIPlayerStrategy(overSeven));
        }

        // Give them starting resources and settlements
        StandardGameSetup.seedInitialState(board, alice, bob, charlie, diana);
        for (Player p : players) {
            p.addResource(ResourceType.WOOD, 5);
            p.addResource(ResourceType.BRICK, 5);
        }

        // 3. Run the engine! The AI will automatically drive the game.
        Path statePath = Files.createTempFile("catan-ai-state-", ".json");
        try (Scanner dummyScanner = new Scanner(System.in)) { // Scanner isn't used by AI, but required by engine
            HumanTurnGameEngine engine = new HumanTurnGameEngine(
                    board, players, dummyScanner, System.out, statePath,
                    () -> new Random().nextInt(11) + 2, // Random dice rolls
                    new Random()
            );

            // Watch the AI play 25 rounds automatically
            engine.runGame(25);
        }
        System.out.println("Demo Complete!");
    }
}
