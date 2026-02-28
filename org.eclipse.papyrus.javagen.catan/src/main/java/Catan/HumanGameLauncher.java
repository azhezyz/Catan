package Catan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public final class HumanGameLauncher {
    private HumanGameLauncher() {
    }

    public static void main(String[] args) {
        Path configPath = args.length >= 1 ? Path.of(args[0]) : Path.of("game.config");
        Path statePath = args.length >= 2 ? Path.of(args[1]) : Path.of("visualize", "state.json");
        HumanGameConfig config = HumanGameConfig.load(configPath);

        try (Scanner scanner = new Scanner(System.in)) {
            List<String> names = promptPlayerNames(scanner);

            Board board = StandardGameSetup.buildFullBoard();
            Player alice = new Player(names.get(0));
            Player bob = new Player(names.get(1));
            Player charlie = new Player(names.get(2));
            Player diana = new Player(names.get(3));
            List<Player> players = List.of(alice, bob, charlie, diana);
            StandardGameSetup.seedInitialState(board, alice, bob, charlie, diana);

            Process visualizer = startVisualizerProcess(statePath);
            try {
                HumanTurnGameEngine engine = new HumanTurnGameEngine(board, players, scanner, System.out, statePath);
                engine.runGame(config.getTurns());
            } finally {
                stopVisualizer(visualizer);
            }
        }
    }

    private static List<String> promptPlayerNames(Scanner scanner) {
        System.out.println("=======================================");
        System.out.println("      CATAN - Human Game Start");
        System.out.println("=======================================");
        System.out.println("Enter player names. Leave blank to use default names.");

        String p1 = readName(scanner, "Player 1", "Alice");
        String p2 = readName(scanner, "Player 2", "Bob");
        String p3 = readName(scanner, "Player 3", "Charlie");
        String p4 = readName(scanner, "Player 4", "Diana");

        System.out.println("Players: " + p1 + ", " + p2 + ", " + p3 + ", " + p4);
        System.out.println("Game starts now.");
        return List.of(p1, p2, p3, p4);
    }

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

    private static File resolveVisualizeDir(Path statePath) {
        Path absoluteState = statePath.toAbsolutePath().normalize();
        Path parent = absoluteState.getParent();
        if (parent != null && parent.getFileName() != null && "visualize".equals(parent.getFileName().toString())) {
            return parent.toFile();
        }
        return new File(System.getProperty("user.dir"), "visualize");
    }

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

    private static void stopVisualizer(Process visualizer) {
        if (visualizer == null) {
            return;
        }
        if (visualizer.isAlive()) {
            visualizer.destroy();
        }
    }
}
