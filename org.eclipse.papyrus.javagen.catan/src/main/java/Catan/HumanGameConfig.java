package Catan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HumanGameConfig {
    private static final Pattern TURNS_LINE = Pattern.compile("^\\s*turns\\s*:\\s*(\\d+)\\s*$");
    private final int turns;

    private HumanGameConfig(int turns) {
        if (turns < 1 || turns > 8192) {
            throw new IllegalArgumentException("turns must be in [1, 8192].");
        }
        this.turns = turns;
    }

    public static HumanGameConfig load(Path path) {
        if (!Files.exists(path)) {
            return new HumanGameConfig(100);
        }
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                Matcher matcher = TURNS_LINE.matcher(trimmed);
                if (matcher.matches()) {
                    return new HumanGameConfig(Integer.parseInt(matcher.group(1)));
                }
            }
            throw new IllegalArgumentException("Config must contain: turns: <int>");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config file: " + path, e);
        }
    }

    public int getTurns() {
        return turns;
    }
}
