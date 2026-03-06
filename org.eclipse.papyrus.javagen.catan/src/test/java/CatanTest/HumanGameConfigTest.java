package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import Catan.HumanGameConfig;

class HumanGameConfigTest {

    @Test
    void loadUsesDefaultWhenFileMissing() {
        Path missing = Path.of("target", "tmp", "missing-human-config.txt");
        HumanGameConfig config = HumanGameConfig.load(missing);
        assertEquals(100, config.getTurns());
    }

    @Test
    void loadParsesTurnsLineWithComments() throws Exception {
        Path file = Files.createTempFile("human-config", ".txt");
        Files.writeString(file, "# comment\n\n turns : 123 \n");

        HumanGameConfig config = HumanGameConfig.load(file);
        assertEquals(123, config.getTurns());
    }

    @Test
    void loadRejectsMissingTurnsDirective() throws Exception {
        Path file = Files.createTempFile("human-config-invalid", ".txt");
        Files.writeString(file, "# only comments\nfoo: 12\n");

        assertThrows(IllegalArgumentException.class, () -> HumanGameConfig.load(file));
    }
}
