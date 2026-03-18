package Catan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
    private HumanGameLauncher() {
    }

    public static void main(String[] args) {
        // Allow overriding config/state paths from CLI; keep local defaults for easy startup.
        Path configPath = args.length >= 1 ? Path.of(args[0]) : Path.of("game.config");
        Path statePath = args.length >= 2 ? Path.of(args[1]) : Path.of("visualize", "state.json");
        HumanGameConfig config = HumanGameConfig.load(configPath);

        try (Scanner scanner = new Scanner(System.in)) {
            List<String> names = promptPlayerNames(scanner);

            // Build a standard board and attach four human players in fixed color order.
            Board board = StandardGameSetup.buildFullBoard();
            Player alice = new Player(names.get(0));
            Player bob = new Player(names.get(1));
            Player charlie = new Player(names.get(2));
            Player diana = new Player(names.get(3));
            List<Player> players = List.of(alice, bob, charlie, diana);
            StandardGameSetup.seedInitialState(board, alice, bob, charlie, diana);

            // Keep the visualizer lifecycle bound to the game lifecycle.
            Process visualizer = startVisualizerProcess(statePath);
            try {
                HumanTurnGameEngine engine = new HumanTurnGameEngine(board, players, scanner, System.out, statePath);
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
