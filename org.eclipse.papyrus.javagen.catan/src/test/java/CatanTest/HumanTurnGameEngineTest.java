package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
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

    @Test
    void runGameCoversBuildAndInvalidCommandBranches() throws Exception {
        Board board = minimalBoard();
        Player alice = new Player("alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WHEAT, 10);
        alice.addResource(ResourceType.ORE, 10);
        alice.addResource(ResourceType.WOOD, 10);
        alice.addResource(ResourceType.BRICK, 10);
        alice.addResource(ResourceType.SHEEP, 10);

        String input = String.join("\n",
                "List",
                "Actions",
                "Go",
                "Build city 0",
                "foo",
                "Roll",
                "Roll",
                "Build city abc",
                "Build city 0",
                "Build road 0",
                "Build castle 1",
                "Build road 0,2",
                "Build road 0,1",
                "Build road 0,1",
                "Build settlement 1",
                "Go"
        ) + "\n";

        java.nio.file.Path state = Files.createTempFile("state-branches", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                board,
                List.of(alice),
                new Scanner(input),
                new PrintStream(sink, true, StandardCharsets.UTF_8),
                state
        );

        engine.runGame(1);

        String log = sink.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Invalid: roll first"));
        assertTrue(log.contains("Invalid command"));
        assertTrue(log.contains("Invalid: already rolled"));
        assertTrue(log.contains("Invalid number format"));
        assertTrue(log.contains("Build city 0"));
        assertTrue(log.contains("Build road failed: use fromNodeId, toNodeId"));
        assertTrue(log.contains("Invalid build type"));
        assertTrue(log.contains("Build road 0,2 failed: no path"));
        assertTrue(log.contains("Build road 0,1"));
        assertTrue(log.contains("Build settlement 1 failed: illegal placement"));

        String json = Files.readString(state);
        assertTrue(json.contains("\"owner\": \"RED\""));
        assertTrue(json.contains("\"type\": \"CITY\""));
    }

    @Test
    void runGameParsesRegexCommandsWithMixedCaseAndFlexibleSpacing() throws Exception {
        Board board = minimalBoard();
        Player alice = new Player("alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WHEAT, 10);
        alice.addResource(ResourceType.ORE, 10);
        alice.addResource(ResourceType.WOOD, 10);
        alice.addResource(ResourceType.BRICK, 10);
        alice.addResource(ResourceType.SHEEP, 10);

        String input = String.join("\n",
                "aCtIoNs",
                "rOlL",
                "bUiLd    road   0 , 1",
                "BuIlD   cItY   0",
                "gO"
        ) + "\n";

        java.nio.file.Path state = Files.createTempFile("state-regex", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                board,
                List.of(alice),
                new Scanner(input),
                new PrintStream(sink, true, StandardCharsets.UTF_8),
                state
        );

        engine.runGame(1);

        String log = sink.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Available actions: Roll | List | Actions"));
        assertTrue(log.contains("Build road 0,1"));
        assertTrue(log.contains("Build city 0"));
        assertTrue(log.contains(": Go"));
    }

    @Test
    void runGameEndsImmediatelyOnWin() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        for (int i = 0; i < 5; i++) {
                board.getNode(i).claim(alice);
                alice.addSettlement(i);
                board.getNode(i).upgradeToCity(alice);
                alice.addCity(i);
        }

        java.nio.file.Path state = Files.createTempFile("win-test", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                board, List.of(alice), new Scanner("Roll\nGo\n"),
                new PrintStream(sink), state
        );

        engine.runGame(10);
        String log = sink.toString();
        assertTrue(log.contains("Win with 10 VP"));
    }

    @Test
    void buildCommandsFailWhenResourcesMissing() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);

        String input = "Roll\nBuild settlement 2\nBuild city 0\nBuild road 0,1\nGo\n";
    
        java.nio.file.Path state = Files.createTempFile("fail-build", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                board, List.of(alice), new Scanner(input),
                new PrintStream(sink), state
        );

        engine.runGame(1);
        String log = sink.toString();
        assertTrue(log.contains("failed: insufficient resources"));
    }

    @Test
    void robberTriggersDiscardAndSteal() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        
        for (int i = 0; i < 10; i++) bob.addResource(ResourceType.WOOD, 1);
        board.getNode(0).claim(bob); 
        bob.addSettlement(0);

        java.nio.file.Path state = Files.createTempFile("robber-test", ".json");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        
        HumanTurnGameEngine engine = new HumanTurnGameEngine(
                board, List.of(alice, bob), new Scanner("Roll\nGo\n"),
                new PrintStream(sink), state, () -> 7, new Random(0)
        );

        engine.runGame(1);
        String log = sink.toString();
        assertTrue(log.contains("Robber activated"));
        assertTrue(log.contains("Discard 5 cards"));
    }
}
