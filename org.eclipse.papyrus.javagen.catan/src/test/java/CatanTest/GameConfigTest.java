package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import Catan.GameConfig;

class GameConfigTest {

    @Test
    void loadUsesDefaultWhenFileMissing() {
        Path missing = Path.of("target", "tmp", "missing-config.properties");
        GameConfig config = GameConfig.load(missing);
        assertEquals(10, config.getMaxRounds());
    }

    @Test
    void loadReadsConfiguredValue() throws Exception {
        Path file = Files.createTempFile("game-config", ".properties");
        Files.writeString(file, "maxRounds=42\n");

        GameConfig config = GameConfig.load(file);
        assertEquals(42, config.getMaxRounds());
    }

    @Test
    void loadRejectsOutOfRangeValue() throws Exception {
        Path file = Files.createTempFile("game-config-invalid", ".properties");
        Files.writeString(file, "maxRounds=0\n");

        assertThrows(IllegalArgumentException.class, () -> GameConfig.load(file));
    }
}
