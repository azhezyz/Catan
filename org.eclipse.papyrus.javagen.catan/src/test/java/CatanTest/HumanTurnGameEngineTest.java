package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

import Catan.Board;
import Catan.BuildAction;
import Catan.BuildingType;
import Catan.ConsoleObserver;
import Catan.GameEvent;
import Catan.GameObserver;
import Catan.HumanTurnGameEngine;
import Catan.Node;
import Catan.Path;
import Catan.Player;
import Catan.ResourceType;
import Catan.StandardGameSetup;
import Catan.StateSnapshotObserver;
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

    private static HumanTurnGameEngine buildEngine(
            Board board,
            List<Player> players,
            String commands,
            java.nio.file.Path statePath,
            RecordingObserver recordingObserver
    ) {
        return buildEngine(board, players, commands, statePath, recordingObserver, () -> 6, new Random(0));
    }

    private static HumanTurnGameEngine buildEngine(
            Board board,
            List<Player> players,
            String commands,
            java.nio.file.Path statePath,
            RecordingObserver recordingObserver,
            IntSupplier rollSupplier,
            Random random
    ) {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        List<GameObserver> observers = List.of(
                new ConsoleObserver(new PrintStream(sink, true, StandardCharsets.UTF_8)),
                new StateSnapshotObserver(statePath),
                recordingObserver
        );
        return new HumanTurnGameEngine(
                board,
                players,
                new Scanner(commands),
                new PrintStream(sink, true, StandardCharsets.UTF_8),
                observers,
                rollSupplier,
                random
        );
    }

    @Test
    void runGameRejectsInvalidTurnBounds() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                "",
                state,
                recorder
        );

        assertThrows(IllegalArgumentException.class, () -> engine.runGame(0));
        assertThrows(IllegalArgumentException.class, () -> engine.runGame(8193));
    }

    @Test
    void runGameHandlesClosedInputAndEmitsInputClosedEvent() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state-closed", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                "",
                state,
                recorder
        );

        engine.runGame(1);

        List<GameEvent.GameEndedEvent> endedEvents = recorder.eventsOfType(GameEvent.GameEndedEvent.class);
        assertEquals(1, endedEvents.size());
        assertEquals(GameEvent.GameEndReason.INPUT_CLOSED, endedEvents.get(0).reason());

        List<GameEvent.StateChangedEvent> snapshotEvents = recorder.eventsOfType(GameEvent.StateChangedEvent.class);
        assertTrue(snapshotEvents.size() >= 1);

        assertTrue(Files.exists(state));
        String json = Files.readString(state);
        assertTrue(json.contains("\"roads\""));
        assertTrue(json.contains("\"buildings\""));
    }

    @Test
    void runGameAcceptsRollThenGoAndEmitsDiceAndTurnEvents() throws Exception {
        java.nio.file.Path state = Files.createTempFile("state-flow", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                minimalBoard(),
                List.of(new Player("alice")),
                "Roll\nGo\n",
                state,
                recorder,
                () -> 6,
                new Random(0)
        );

        engine.runGame(1);

        assertEquals(1, recorder.eventsOfType(GameEvent.TurnStartEvent.class).size());
        List<GameEvent.DiceRolledEvent> rollEvents = recorder.eventsOfType(GameEvent.DiceRolledEvent.class);
        assertEquals(1, rollEvents.size());
        assertEquals(6, rollEvents.get(0).roll());

        List<GameEvent.GameEndedEvent> endedEvents = recorder.eventsOfType(GameEvent.GameEndedEvent.class);
        assertEquals(1, endedEvents.size());
        assertEquals(GameEvent.GameEndReason.MAX_TURNS, endedEvents.get(0).reason());

        List<String> actions = recorder.actionMessages();
        assertTrue(actions.stream().anyMatch(m -> m.startsWith("Roll ")));
        assertTrue(actions.contains("Go"));
    }

    @Test
    void runGameCoversBuildAndInvalidCommandBranchesViaEvents() throws Exception {
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
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                input,
                state,
                recorder,
                () -> 4,
                new Random(0)
        );

        engine.runGame(1);

        List<String> messages = recorder.actionMessages();
        assertTrue(messages.contains("Invalid: roll first"));
        assertTrue(messages.contains("Invalid command"));
        assertTrue(messages.contains("Invalid: already rolled"));
        assertTrue(messages.contains("Invalid number format"));
        assertTrue(messages.contains("Build city 0"));
        assertTrue(messages.contains("Build road failed: use fromNodeId, toNodeId"));
        assertTrue(messages.contains("Invalid build type"));
        assertTrue(messages.contains("Build road 0,2 failed: no path"));
        assertTrue(messages.contains("Build road 0,1"));
        assertTrue(messages.contains("Build settlement 1 failed: illegal placement"));

        List<GameEvent.BuildSucceededEvent> buildSucceededEvents = recorder.eventsOfType(GameEvent.BuildSucceededEvent.class);
        assertTrue(buildSucceededEvents.stream().anyMatch(e -> e.buildAction() == BuildAction.CITY));
        assertTrue(buildSucceededEvents.stream().anyMatch(e -> e.buildAction() == BuildAction.ROAD));

        List<GameEvent.BuildFailedEvent> buildFailedEvents = recorder.eventsOfType(GameEvent.BuildFailedEvent.class);
        assertTrue(buildFailedEvents.stream().anyMatch(e -> e.reason().equals("no path")));
        assertTrue(buildFailedEvents.stream().anyMatch(e -> e.reason().equals("illegal placement")));

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
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                input,
                state,
                recorder,
                () -> 5,
                new Random(0)
        );

        engine.runGame(1);

        List<GameEvent.BuildSucceededEvent> buildSucceededEvents = recorder.eventsOfType(GameEvent.BuildSucceededEvent.class);
        assertTrue(buildSucceededEvents.stream().anyMatch(e -> e.buildAction() == BuildAction.ROAD));
        assertTrue(buildSucceededEvents.stream().anyMatch(e -> e.buildAction() == BuildAction.CITY));
        assertTrue(recorder.actionMessages().contains("Go"));
    }

    @Test
    void runGameEndsImmediatelyOnWinAndEmitsWinEvent() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        for (int i = 0; i < 5; i++) {
            board.getNode(i).claim(alice);
            alice.addSettlement(i);
            board.getNode(i).upgradeToCity(alice);
            alice.addCity(i);
        }

        java.nio.file.Path state = Files.createTempFile("win-test", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                "Roll\nGo\n",
                state,
                recorder,
                () -> 6,
                new Random(0)
        );

        engine.runGame(10);

        List<GameEvent.GameEndedEvent> endedEvents = recorder.eventsOfType(GameEvent.GameEndedEvent.class);
        assertEquals(1, endedEvents.size());
        assertEquals(GameEvent.GameEndReason.WIN, endedEvents.get(0).reason());
        assertEquals("Alice", endedEvents.get(0).playerName());
        assertTrue(endedEvents.get(0).victoryPoints() >= 10);
    }

    @Test
    void buildCommandsFailWhenResourcesMissingAndEmitBuildFailedEvent() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);

        String input = "Roll\nBuild settlement 2\nBuild city 0\nBuild road 0,1\nGo\n";

        java.nio.file.Path state = Files.createTempFile("fail-build", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                input,
                state,
                recorder,
                () -> 6,
                new Random(0)
        );

        engine.runGame(1);

        List<GameEvent.BuildFailedEvent> buildFailedEvents = recorder.eventsOfType(GameEvent.BuildFailedEvent.class);
        assertTrue(buildFailedEvents.stream().anyMatch(e -> e.reason().equals("insufficient resources")));
    }

    @Test
    void robberTriggersDiscardAndStealAndEmitsRobberEvents() throws Exception {
        Board board = StandardGameSetup.buildFullBoard();
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");

        for (int i = 0; i < 10; i++) {
            alice.addResource(ResourceType.WOOD, 1);
        }

        board.getNode(0).claim(bob);
        bob.addSettlement(0);
        bob.addResource(ResourceType.SHEEP, 1);

        java.nio.file.Path state = Files.createTempFile("robber-test", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice, bob),
                "Roll\nGo\n",
                state,
                recorder,
                () -> 7,
                new Random(0)
        );

        engine.runGame(1);

        assertEquals(1, recorder.eventsOfType(GameEvent.RobberMovedEvent.class).size());
        List<GameEvent.RobbedEvent> robbedEvents = recorder.eventsOfType(GameEvent.RobbedEvent.class);
        assertEquals(1, robbedEvents.size());
        assertEquals(ResourceType.SHEEP, robbedEvents.get(0).resourceType());

        List<String> messages = recorder.actionMessages();
        assertTrue(messages.contains("Robber activated"));
        assertTrue(messages.contains("Discard 5 cards"));
    }

    @Test
    void undoRedoReplaysBuildRoadCommandAndEmitsUndoRedoEvents() throws Exception {
        Board board = minimalBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WOOD, 2);
        alice.addResource(ResourceType.BRICK, 2);

        java.nio.file.Path state = Files.createTempFile("undo-redo-road", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                "Roll\nBuild road 0,1\nUndo\nRedo\nGo\n",
                state,
                recorder,
                () -> 2,
                new Random(0)
        );

        engine.runGame(1);

        assertTrue(board.getPath(0).isOwnedBy(alice));
        assertEquals(1, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(1, alice.getResourceCount(ResourceType.BRICK));

        List<GameEvent.UndoRedoEvent> undoRedoEvents = recorder.eventsOfType(GameEvent.UndoRedoEvent.class);
        assertTrue(undoRedoEvents.stream().anyMatch(e -> e.type() == GameEvent.UndoRedoType.UNDO && e.success()));
        assertTrue(undoRedoEvents.stream().anyMatch(e -> e.type() == GameEvent.UndoRedoType.REDO && e.success()));
    }

    @Test
    void redoHistoryClearsAfterExecutingNewCommandAndEmitsFailedRedoEvent() throws Exception {
        Board board = minimalBoard();
        Player alice = new Player("Alice");
        board.getNode(0).claim(alice);
        alice.addSettlement(0);
        alice.addResource(ResourceType.WOOD, 2);
        alice.addResource(ResourceType.BRICK, 2);
        alice.addResource(ResourceType.WHEAT, 3);
        alice.addResource(ResourceType.ORE, 4);

        java.nio.file.Path state = Files.createTempFile("redo-clears", ".json");
        RecordingObserver recorder = new RecordingObserver();
        HumanTurnGameEngine engine = buildEngine(
                board,
                List.of(alice),
                "Roll\nBuild road 0,1\nUndo\nBuild city 0\nRedo\nGo\n",
                state,
                recorder,
                () -> 2,
                new Random(0)
        );

        engine.runGame(1);

        assertFalse(board.getPath(0).isOwnedBy(alice));
        assertEquals(2, alice.getResourceCount(ResourceType.WOOD));
        assertEquals(2, alice.getResourceCount(ResourceType.BRICK));
        assertEquals(1, alice.getResourceCount(ResourceType.WHEAT));
        assertEquals(1, alice.getResourceCount(ResourceType.ORE));
        assertEquals(BuildingType.CITY, board.getNode(0).getBuilding().getType());

        List<GameEvent.UndoRedoEvent> undoRedoEvents = recorder.eventsOfType(GameEvent.UndoRedoEvent.class);
        assertTrue(undoRedoEvents.stream().anyMatch(
                e -> e.type() == GameEvent.UndoRedoType.REDO && !e.success() && e.detail().equals("nothing to redo")
        ));
    }

    private static final class RecordingObserver implements GameObserver {
        private final List<GameEvent> events = new ArrayList<>();

        @Override
        public void onEvent(GameEvent event) {
            events.add(event);
        }

        private <T extends GameEvent> List<T> eventsOfType(Class<T> type) {
            List<T> typed = new ArrayList<>();
            for (GameEvent event : events) {
                if (type.isInstance(event)) {
                    typed.add(type.cast(event));
                }
            }
            return typed;
        }

        private List<String> actionMessages() {
            List<String> messages = new ArrayList<>();
            for (GameEvent event : events) {
                if (event instanceof GameEvent.ActionEvent actionEvent) {
                    messages.add(actionEvent.action());
                }
            }
            return messages;
        }
    }
}
