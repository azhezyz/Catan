package Catan;

import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Text-UI demo that showcases major project features in deterministic scenarios.
 * It is intended for demonstration only.
 */
public final class FeatureShowcaseDemo {
    private FeatureShowcaseDemo() {
    }

    public static void main(String[] args) throws Exception {
        try (Scanner ui = new Scanner(System.in)) {
            while (true) {
                printMenu();
                String choice = ui.hasNextLine() ? ui.nextLine().trim() : "0";
                if ("0".equals(choice)) {
                    return;
                }
                switch (choice) {
                    case "1" -> runScenarioBasicCommands(System.out);
                    case "2" -> runScenarioBuildAndLongestRoad(System.out);
                    case "3" -> runScenarioRobberAndBlock(System.out);
                    case "4" -> runScenarioWin(System.out);
                    case "9" -> runAll(System.out);
                    default -> System.out.println("Unknown option.");
                }
                System.out.println();
                System.out.println("Press Enter to continue...");
                if (ui.hasNextLine()) {
                    ui.nextLine();
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("==============================================");
        System.out.println(" Catan Feature Showcase (Deterministic Demo) ");
        System.out.println("==============================================");
        System.out.println("1) Basic command flow + validation");
        System.out.println("2) Resource collect + build + longest road");
        System.out.println("3) Robber (discard/move/steal) + block production");
        System.out.println("4) Win condition trigger");
        System.out.println("9) Run all scenarios");
        System.out.println("0) Exit");
        System.out.print("Select: ");
    }

    /**
     * Runs all showcase scenarios in a fixed order.
     *
     * <p>Useful when you want a full regression-style smoke run of the text UI behaviors
     * without manually selecting each menu option.
     */
    private static void runAll(PrintStream out) throws Exception {
        runScenarioBasicCommands(out);
        runScenarioBuildAndLongestRoad(out);
        runScenarioRobberAndBlock(out);
        runScenarioWin(out);
    }

    /**
     * Simulates a minimal one-player turn to verify command parsing and turn-order validation.
     *
     * <p>Setup:
     * <ul>
     *     <li>Smallest possible board: 1 tile, 2 nodes, 1 edge.</li>
     *     <li>Alice starts with exactly enough resources for one road.</li>
     * </ul>
     *
     * <p>Scripted input intentionally mixes valid/invalid operations:
     * <ul>
     *     <li>{@code Go} before rolling (should be rejected by turn flow rules).</li>
     *     <li>Build attempts before/after roll with malformed or impossible coordinates.</li>
     *     <li>Duplicate roll in one turn (should be rejected).</li>
     *     <li>One valid road build, then normal turn end.</li>
     * </ul>
     *
     * <p>This scenario is mainly a parser + state-machine sanity check for command handling.
     */
    private static void runScenarioBasicCommands(PrintStream out) throws Exception {
        out.println();
        out.println("=== Scenario 1: Basic command flow + validation ===");
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 6, Set.of(0, 1))),
                List.of(
                        new Node(0, Set.of(0), Set.of(1)),
                        new Node(1, Set.of(0), Set.of(0))
                ),
                List.of(new Path(0, 0, 1))
        );
        Player alice = new Player("Alice");
        alice.addResource(ResourceType.WOOD, 1);
        alice.addResource(ResourceType.BRICK, 1);

        String commands = String.join("\n",
                "List",
                "Go",
                "Build road 0,1",
                "Roll",
                "Roll",
                "Build road 0",
                "Build road 0,2",
                "Build road 0,1",
                "Go"
        ) + "\n";

        runEngine(
                out,
                board,
                List.of(alice),
                commands,
                new FixedRolls(6),
                new ScriptedRandom(0),
                1
        );
    }

    /**
     * Simulates a deterministic "economic turn" that exercises production and chained building.
     *
     * <p>Setup:
     * <ul>
     *     <li>Linear board so road connectivity and path extension are easy to reason about.</li>
     *     <li>Alice already owns an initial settlement + road to satisfy adjacency requirements.</li>
     *     <li>Resource inventory is preloaded to cover multiple roads, one settlement, one city.</li>
     * </ul>
     *
     * <p>What is being demonstrated:
     * <ul>
     *     <li>Deterministic roll ({@code 8}) triggers predictable resource collection.</li>
     *     <li>Sequential road builds extend a continuous chain to update longest-road logic.</li>
     *     <li>Settlement placement and city upgrade validate build rule paths in one turn.</li>
     *     <li>{@code Actions}/{@code List} prints confirm resulting state and available commands.</li>
     * </ul>
     */
    private static void runScenarioBuildAndLongestRoad(PrintStream out) throws Exception {
        out.println();
        out.println("=== Scenario 2: Resource collect + build + longest road ===");
        Board board = buildLinearBuildBoard();
        Player alice = new Player("Alice");

        claimSettlement(board, alice, 0);
        claimRoad(board, alice, 0);

        alice.addResource(ResourceType.WOOD, 5);
        alice.addResource(ResourceType.BRICK, 5);
        alice.addResource(ResourceType.SHEEP, 1);
        alice.addResource(ResourceType.WHEAT, 3);
        alice.addResource(ResourceType.ORE, 3);

        String commands = String.join("\n",
                "Roll",
                "Build road 1,2",
                "Build road 2,3",
                "Build road 3,4",
                "Build road 4,5",
                "Build settlement 5",
                "Build city 0",
                "Actions",
                "List",
                "Go"
        ) + "\n";

        runEngine(
                out,
                board,
                List.of(alice),
                commands,
                new FixedRolls(8),
                new ScriptedRandom(0),
                1
        );
    }

    /**
     * Simulates the robber pipeline end-to-end across two players.
     *
     * <p>Setup:
     * <ul>
     *     <li>Board contains a desert tile and one productive wood tile (number 6).</li>
     *     <li>Bob starts with 8 cards to force discard behavior when 7 is rolled.</li>
     *     <li>Scripted random values make robber target/steal outcomes deterministic.</li>
     * </ul>
     *
     * <p>What is being demonstrated:
     * <ul>
     *     <li>First roll is {@code 7}: triggers discard checks and robber move/steal flow.</li>
     *     <li>Second roll is {@code 6}: validates that robber placement can block production.</li>
     *     <li>{@code List} after these actions lets you inspect card/board effects.</li>
     * </ul>
     */
    private static void runScenarioRobberAndBlock(PrintStream out) throws Exception {
        out.println();
        out.println("=== Scenario 3: Robber discard/move/steal + block production ===");
        Board board = buildRobberBlockBoard();
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");

        claimSettlement(board, alice, 0);
        claimSettlement(board, bob, 1);
        bob.addResource(ResourceType.WOOD, 8);

        String commands = String.join("\n",
                "Roll",
                "Go",
                "Roll",
                "List",
                "Go"
        ) + "\n";

        runEngine(
                out,
                board,
                List.of(alice, bob),
                commands,
                new FixedRolls(7, 6),
                new ScriptedRandom(0, 0, 0, 0, 0),
                1
        );
    }

    /**
     * Simulates immediate victory detection from a pre-seeded high-score board state.
     *
     * <p>Setup:
     * <ul>
     *     <li>Uses the full standard board to match normal gameplay topology.</li>
     *     <li>Alice is granted multiple cities before the turn starts to reach win threshold.</li>
     * </ul>
     *
     * <p>The short script ({@code Roll -> Go}) is intentional: it checks that win-condition
     * evaluation still fires correctly even when the turn itself has minimal actions.
     */
    private static void runScenarioWin(PrintStream out) throws Exception {
        out.println();
        out.println("=== Scenario 4: Win condition trigger ===");
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");

        grantCity(board, alice, 0);
        grantCity(board, alice, 3);
        grantCity(board, alice, 6);
        grantCity(board, alice, 10);
        grantCity(board, alice, 14);

        String commands = "Roll\nGo\n";

        runEngine(
                out,
                board,
                List.of(alice),
                commands,
                new FixedRolls(8),
                new ScriptedRandom(0),
                1
        );
    }

    /**
     * Boots a HumanTurnGameEngine with scripted input and deterministic randomness.
     *
     * <p>Every scenario passes its own command stream, dice supplier, and Random implementation
     * so demo output is reproducible and suitable for repeatable manual verification.
     */
    private static void runEngine(
            PrintStream out,
            Board board,
            List<Player> players,
            String commands,
            IntSupplier rolls,
            Random random,
            int turns
    ) throws Exception {
        java.nio.file.Path state = Files.createTempFile("catan-demo-state-", ".json");
        out.println("State file: " + state.toAbsolutePath());
        try (Scanner input = new Scanner(commands)) {
            HumanTurnGameEngine engine = new HumanTurnGameEngine(
                    board, players, input, out, state, rolls, random
            );
            engine.runGame(turns);
        }
    }

    /**
     * Linear board used by build/road showcase to keep adjacency reasoning obvious.
     */
    private static Board buildLinearBuildBoard() {
        List<Tile> tiles = List.of(
                new Tile(0, ResourceType.WOOD, 8, Set.of(0, 1, 2)),
                new Tile(1, ResourceType.BRICK, 8, Set.of(2, 3, 4)),
                new Tile(2, ResourceType.WHEAT, 8, Set.of(4, 5, 6)),
                new Tile(3, ResourceType.ORE, 8, Set.of(6, 7, 8))
        );

        List<Node> nodes = List.of(
                new Node(0, Set.of(0), Set.of(1)),
                new Node(1, Set.of(0), Set.of(0, 2)),
                new Node(2, Set.of(0, 1), Set.of(1, 3)),
                new Node(3, Set.of(1), Set.of(2, 4)),
                new Node(4, Set.of(1, 2), Set.of(3, 5)),
                new Node(5, Set.of(2), Set.of(4, 6)),
                new Node(6, Set.of(2, 3), Set.of(5, 7)),
                new Node(7, Set.of(3), Set.of(6, 8)),
                new Node(8, Set.of(3), Set.of(7))
        );

        List<Path> paths = List.of(
                new Path(0, 0, 1),
                new Path(1, 1, 2),
                new Path(2, 2, 3),
                new Path(3, 3, 4),
                new Path(4, 4, 5),
                new Path(5, 5, 6),
                new Path(6, 6, 7),
                new Path(7, 7, 8)
        );
        return new Board(tiles, nodes, paths);
    }

    /**
     * Compact board with one productive tile and one robber tile for robber behavior demos.
     */
    private static Board buildRobberBlockBoard() {
        List<Tile> tiles = List.of(
                new Tile(0, null, 0, Set.of(0, 1, 2)),
                new Tile(1, ResourceType.WOOD, 6, Set.of(1, 2, 3))
        );

        List<Node> nodes = List.of(
                new Node(0, Set.of(0), Set.of(1, 2)),
                new Node(1, Set.of(0, 1), Set.of(0, 2, 3)),
                new Node(2, Set.of(0, 1), Set.of(0, 1, 3)),
                new Node(3, Set.of(1), Set.of(1, 2))
        );

        List<Path> paths = List.of(
                new Path(0, 0, 1),
                new Path(1, 0, 2),
                new Path(2, 1, 2),
                new Path(3, 1, 3),
                new Path(4, 2, 3)
        );
        return new Board(tiles, nodes, paths);
    }

    /**
     * Directly grants a settlement without cost checks (demo setup helper).
     */
    private static void claimSettlement(Board board, Player player, int nodeId) {
        Node node = board.getNode(nodeId);
        node.claim(player);
        player.addSettlement(nodeId);
    }

    /**
     * Directly grants a road without cost checks (demo setup helper).
     */
    private static void claimRoad(Board board, Player player, int pathId) {
        Path path = board.getPath(pathId);
        path.claim(player);
        player.addRoad(pathId);
    }

    /**
     * Directly grants a city by claiming a node then upgrading it (demo setup helper).
     */
    private static void grantCity(Board board, Player player, int nodeId) {
        Node node = board.getNode(nodeId);
        node.claim(player);
        player.addSettlement(nodeId);
        node.upgradeToCity(player);
        player.addCity(nodeId);
    }

    /**
     * Returns scripted dice values in order; once exhausted, repeats the last value.
     */
    private static final class FixedRolls implements IntSupplier {
        private final List<Integer> values;
        private int index = 0;

        private FixedRolls(int... values) {
            if (values.length == 0) {
                throw new IllegalArgumentException("At least one roll value is required.");
            }
            List<Integer> list = new ArrayList<>();
            for (int value : values) {
                list.add(value);
            }
            this.values = List.copyOf(list);
        }

        @Override
        public int getAsInt() {
            int i = Math.min(index, values.size() - 1);
            index++;
            return values.get(i);
        }
    }

    /**
     * Deterministic Random replacement used for predictable robber target and steal choices.
     */
    private static final class ScriptedRandom extends Random {
        private final int[] sequence;
        private int index = 0;

        private ScriptedRandom(int... sequence) {
            this.sequence = sequence.length == 0 ? new int[]{0} : sequence.clone();
        }

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be positive");
            }
            int i = Math.min(index, sequence.length - 1);
            int value = sequence[i];
            index++;
            return Math.floorMod(value, bound);
        }
    }
}
