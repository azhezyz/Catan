package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Catan.Board;
import Catan.HumanTurnGameEngine;
import Catan.Node;
import Catan.Path;
import Catan.Player;
import Catan.ResourceType;
import Catan.Tile;

class HumanTurnGameEngineTest {

    private static Board minimalBoard() {
        return new Board(
                List.of(new Tile(0, ResourceType.WOOD, 6, Set.of(0, 1))),
                List.of(
                        new Node(0, Set.of(0), Set.of(1)),
                        new Node(1, Set.of(0), Set.of(0))
                ),
                List.of(new Path(0, 0, 1))
        );
    }

    @Test
    void runGameRejectsInvalidTurnBounds() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state", ".json");
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                new Scanner(""),
                new PrintStream(new ByteArrayOutputStream()),
                state
        );

        assertThrows(IllegalArgumentException.class, () -> engine.runGame(0));
        assertThrows(IllegalArgumentException.class, () -> engine.runGame(8193));
    }

    @Test
    void runGameHandlesClosedInputAndStillWritesInitialSnapshot() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state-closed", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                new Scanner(""),
                new PrintStream(sink, true, StandardCharsets.UTF_8),
                state
        );

        engine.runGame(1);

        String log = sink.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Input closed. Game ended."));
        assertTrue(Files.exists(state));
        String json = Files.readString(state);
        assertTrue(json.contains("\"roads\""));
        assertTrue(json.contains("\"buildings\""));
    }

    @Test
    void runGameAcceptsRollThenGoFlow() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state-flow", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                new Scanner("Roll\nGo\n"),
                new PrintStream(sink, true, StandardCharsets.UTF_8),
                state
        );

        engine.runGame(1);

        String log = sink.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Turn 1 - alice"));
        assertTrue(log.contains(": Roll "));
        assertTrue(log.contains(": Go"));
        assertTrue(log.contains("Reached max turns without a winner."));
    }
}
