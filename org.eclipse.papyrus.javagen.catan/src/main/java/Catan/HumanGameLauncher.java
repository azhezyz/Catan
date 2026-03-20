package Catan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * Interactive launcher for a human-playable Catan session.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Load launcher configuration (turn count, etc.).</li>
 *     <li>Prompt and validate player display names.</li>
 *     <li>Create the initial board/players and seed default setup.</li>
 *     <li>Optionally start the visualizer process and stop it on exit.</li>
 * </ul>
 */
public final class HumanGameLauncher {
    private static final List<String> PLAYER_COLORS = List.of("Red", "Blue", "Yellow", "White");

    private enum PlayerControlMode {
        HUMAN("0", "Human (manual input)"),
        AI_OVER_SEVEN("1", "AI OverSeven (discard-risk control)"),
        AI_CONNECT_ROADS("2", "AI ConnectRoads (network bridging)"),
        AI_DEFEND_LONGEST_ROAD("3", "AI DefendLongestRoad (road race)"),
        AI_VALUE_MAX("4", "AI ValueMax (highest immediate value)");

        private final String key;
        private final String description;

        PlayerControlMode(String key, String description) {
            this.key = key;
            this.description = description;
        }

        static PlayerControlMode fromChoice(String input) {
            String normalized = input == null ? "" : input.trim();
            if (normalized.isEmpty()) {
                return HUMAN;
            }
            for (PlayerControlMode mode : values()) {
                if (mode.key.equals(normalized)) {
                    return mode;
                }
            }
            return null;
        }
    }

    private HumanGameLauncher() {
    }

    public static void main(String[] args) {
        // Allow overriding config/state paths from CLI; keep local defaults for easy startup.
        Path configPath = args.length >= 1 ? Path.of(args[0]) : Path.of("game.config");
        Path statePath = args.length >= 2 ? Path.of(args[1]) : Path.of("visualize", "state.json");
        HumanGameConfig config = HumanGameConfig.load(configPath);

        try (Scanner scanner = new Scanner(System.in)) {
            List<String> names = promptPlayerNames(scanner);
            List<PlayerControlMode> controlModes = promptPlayerControlModes(scanner, names);

            // Build a standard board and attach four configured players in fixed color order.
            Board board = StandardGameSetup.buildFullBoard();
            List<Player> players = createPlayers(names, controlModes);
            Player alice = players.get(0);
            Player bob = players.get(1);
            Player charlie = players.get(2);
            Player diana = players.get(3);
            StandardGameSetup.seedInitialState(board, alice, bob, charlie, diana);

            // Keep the visualizer lifecycle bound to the game lifecycle.
            Process visualizer = startVisualizerProcess(statePath);
            try {
                List<GameObserver> observers = List.of(
                        new ConsoleObserver(System.out),
                        new StateSnapshotObserver(statePath)
                );
                HumanTurnGameEngine engine = new HumanTurnGameEngine(board, players, scanner, System.out, observers);
                engine.runGame(config.getTurns());
            } finally {
                stopVisualizer(visualizer);
            }
        }
    }

    /**
     * Collects four player names from stdin, applying defaults when input is blank or unavailable.
     */
    private static List<String> promptPlayerNames(Scanner scanner) {
        System.out.println("=======================================");
        System.out.println("      CATAN - Human Game Start");
        System.out.println("=======================================");
        System.out.println("Enter player names. Leave blank to use default names.");

        String p1 = readName(scanner, "Red", "Alice");
        String p2 = readName(scanner, "Blue", "Bob");
        String p3 = readName(scanner, "Yellow", "Charlie");
        String p4 = readName(scanner, "White", "Diana");

        System.out.println("Players: " + p1 + ", " + p2 + ", " + p3 + ", " + p4);
        System.out.println("Game starts now.");
        return List.of(p1, p2, p3, p4);
    }

    /**
     * Lets each player seat choose a controller mode.
     *
     * <p>Default (blank input) is human controller.
     */
    private static List<PlayerControlMode> promptPlayerControlModes(Scanner scanner, List<String> names) {
        if (names.size() != 4) {
            throw new IllegalArgumentException("Exactly 4 player names are required.");
        }

        System.out.println();
        System.out.println("Select controller for each seat:");
        for (PlayerControlMode mode : PlayerControlMode.values()) {
            System.out.println("  " + mode.key + " = " + mode.description);
        }
        System.out.println("Leave blank to use 0 (Human).");

        List<PlayerControlMode> modes = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            modes.add(readControlMode(scanner, PLAYER_COLORS.get(i), names.get(i)));
        }

        System.out.println("Controller setup:");
        for (int i = 0; i < 4; i++) {
            System.out.println("  " + PLAYER_COLORS.get(i) + " (" + names.get(i) + "): " + modes.get(i).description);
        }
        System.out.println();
        return List.copyOf(modes);
    }

    private static PlayerControlMode readControlMode(Scanner scanner, String colorLabel, String playerName) {
        while (true) {
            System.out.print(colorLabel + " [" + playerName + "] controller (0-4): ");
            if (!scanner.hasNextLine()) {
                return PlayerControlMode.HUMAN;
            }
            PlayerControlMode mode = PlayerControlMode.fromChoice(scanner.nextLine());
            if (mode != null) {
                return mode;
            }
            System.out.println("Invalid option. Enter 0, 1, 2, 3, 4, or blank.");
        }
    }

    private static List<Player> createPlayers(List<String> names, List<PlayerControlMode> modes) {
        Objects.requireNonNull(names, "names");
        Objects.requireNonNull(modes, "modes");
        if (names.size() != 4 || modes.size() != 4) {
            throw new IllegalArgumentException("Exactly 4 names and 4 control modes are required.");
        }

        List<Player> players = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Player player = new Player(names.get(i));
            player.setStrategy(strategyFor(modes.get(i)));
            players.add(player);
        }
        return List.copyOf(players);
    }

    private static PlayerStrategy strategyFor(PlayerControlMode mode) {
        return switch (mode) {
            case HUMAN -> new HumanPlayerStrategy();
            case AI_OVER_SEVEN -> new AIPlayerStrategy(chain(
                    new OverSevenCardsHandler(),
                    new ValueMaximizationHandler(new Random())
            ));
            case AI_CONNECT_ROADS -> new AIPlayerStrategy(chain(
                    new ConnectRoadsHandler(),
                    new ValueMaximizationHandler(new Random())
            ));
            case AI_DEFEND_LONGEST_ROAD -> new AIPlayerStrategy(chain(
                    new DefendLongestRoadHandler(),
                    new ValueMaximizationHandler(new Random())
            ));
            case AI_VALUE_MAX -> new AIPlayerStrategy(new ValueMaximizationHandler(new Random()));
        };
    }

    private static BotRuleHandler chain(BotRuleHandler first, BotRuleHandler second) {
        first.setNext(second);
        return first;
    }

    /**
     * Reads and validates one player name.
     *
     * <p>Rules: blank => default name, max length 40.
     */
    private static String readName(Scanner scanner, String label, String defaultName) {
        while (true) {
            System.out.print(label + " name: ");
            if (!scanner.hasNextLine()) {
                return defaultName;
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultName;
            }
            if (input.length() > 40) {
                System.out.println("Name too long. Max 40 characters.");
                continue;
            }
            return input;
        }
    }

    /**
     * Starts the Python visualizer in watch mode when its runtime is available.
     */
    private static Process startVisualizerProcess(Path statePath) {
        File visualizeDir = resolveVisualizeDir(statePath);
        if (visualizeDir == null || !visualizeDir.isDirectory()) {
            System.out.println("[Launcher] visualize directory not found. Skip visualizer.");
            return null;
        }

        String python = resolvePythonExecutable(visualizeDir);
        if (python == null) {
            System.out.println("[Launcher] Visualizer Python environment not found. Skip visualizer.");
            return null;
        }

        ProcessBuilder builder = new ProcessBuilder(
                python,
                "light_visualizer.py",
                "base_map.json",
                statePath.toAbsolutePath().toString(),
                "--watch"
        );
        builder.directory(visualizeDir);
        builder.redirectErrorStream(true);
        builder.inheritIO();
        try {
            System.out.println("[Launcher] Starting visualizer after player setup...");
            return builder.start();
        } catch (IOException e) {
            System.out.println("[Launcher] Failed to start visualizer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the visualizer directory from the state path.
     *
     * <p>If state path already points inside "visualize/", use that parent; otherwise fallback to
     * "{user.dir}/visualize".
     */
    private static File resolveVisualizeDir(Path statePath) {
        Path absoluteState = statePath.toAbsolutePath().normalize();
        Path parent = absoluteState.getParent();
        if (parent != null && parent.getFileName() != null && "visualize".equals(parent.getFileName().toString())) {
            return parent.toFile();
        }
        return new File(System.getProperty("user.dir"), "visualize");
    }

    /**
     * Finds the project-local Python executable in ".venv" (Windows or Unix layout).
     */
    private static String resolvePythonExecutable(File visualizeDir) {
        File venvWindowsPython = new File(visualizeDir, ".venv\\Scripts\\python.exe");
        if (venvWindowsPython.isFile()) {
            return venvWindowsPython.getAbsolutePath();
        }
        File venvUnixPython = new File(visualizeDir, ".venv/bin/python");
        if (venvUnixPython.isFile()) {
            return venvUnixPython.getAbsolutePath();
        }
        return null;
    }

    /**
     * Best-effort shutdown for the spawned visualizer process.
     */
    private static void stopVisualizer(Process visualizer) {
        if (visualizer == null) {
            return;
        }
        if (visualizer.isAlive()) {
            visualizer.destroy();
        }
    }
}
