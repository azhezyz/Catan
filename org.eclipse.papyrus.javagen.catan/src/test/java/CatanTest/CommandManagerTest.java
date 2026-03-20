package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import Catan.CommandManager;
import Catan.GameCommand;

class CommandManagerTest {

    @Test
    void executeCommandStoresFailureReasonWhenExecutionFails() {
        CommandManager manager = new CommandManager();
        ScriptedCommand failing = new ScriptedCommand("execute failed", false);

        assertFalse(manager.executeCommand(failing));
        assertEquals("execute failed", manager.getLastFailureReason());
        assertFalse(manager.undoLast());
        assertEquals("nothing to undo", manager.getLastFailureReason());
    }

    @Test
    void undoAndRedoSucceedForExecutedCommand() {
        CommandManager manager = new CommandManager();
        ScriptedCommand command = new ScriptedCommand("unused", true, true);

        assertTrue(manager.executeCommand(command));
        assertEquals("", manager.getLastFailureReason());
        assertTrue(manager.undoLast());
        assertEquals(1, command.undoCalls);
        assertEquals("", manager.getLastFailureReason());
        assertTrue(manager.redoLast());
        assertEquals("", manager.getLastFailureReason());
    }

    @Test
    void redoFailsWhenNoCommandIsAvailable() {
        CommandManager manager = new CommandManager();

        assertFalse(manager.redoLast());
        assertEquals("nothing to redo", manager.getLastFailureReason());
    }

    @Test
    void executeCommandClearsRedoHistory() {
        CommandManager manager = new CommandManager();
        ScriptedCommand first = new ScriptedCommand("unused", true, true);
        ScriptedCommand second = new ScriptedCommand("unused", true);

        assertTrue(manager.executeCommand(first));
        assertTrue(manager.undoLast());
        assertTrue(manager.executeCommand(second));
        assertFalse(manager.redoLast());
        assertEquals("nothing to redo", manager.getLastFailureReason());
    }

    @Test
    void redoFailurePropagatesCommandFailureReason() {
        CommandManager manager = new CommandManager();
        ScriptedCommand command = new ScriptedCommand("redo execute failed", true, false);

        assertTrue(manager.executeCommand(command));
        assertTrue(manager.undoLast());
        assertFalse(manager.redoLast());
        assertEquals("redo execute failed", manager.getLastFailureReason());
        assertFalse(manager.undoLast());
        assertEquals("nothing to undo", manager.getLastFailureReason());
    }

    private static final class ScriptedCommand implements GameCommand {
        private final String failureReason;
        private final boolean[] executeResults;
        private int executeIndex = 0;
        private int undoCalls = 0;

        private ScriptedCommand(String failureReason, boolean... executeResults) {
            this.failureReason = failureReason;
            this.executeResults = executeResults.clone();
        }

        @Override
        public boolean execute() {
            int index = Math.min(executeIndex, executeResults.length - 1);
            executeIndex++;
            return executeResults[index];
        }

        @Override
        public void undo() {
            undoCalls++;
        }

        @Override
        public String getFailureReason() {
            return failureReason;
        }
    }
}

