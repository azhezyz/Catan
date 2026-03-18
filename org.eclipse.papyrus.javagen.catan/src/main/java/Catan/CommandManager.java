package Catan;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandManager {
    private final Deque<GameCommand> undoStack = new ArrayDeque<>();
    private final Deque<GameCommand> redoStack = new ArrayDeque<>();
    private String lastFailureReason = "";

    public boolean executeCommand(GameCommand command) {
        if (!command.execute()) {
            lastFailureReason = command.getFailureReason();
            return false;
        }
        undoStack.push(command);
        redoStack.clear();
        lastFailureReason = "";
        return true;
    }

    public boolean undoLast() {
        if (undoStack.isEmpty()) {
            lastFailureReason = "nothing to undo";
            return false;
        }
        GameCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        lastFailureReason = "";
        return true;
    }

    public boolean redoLast() {
        if (redoStack.isEmpty()) {
            lastFailureReason = "nothing to redo";
            return false;
        }
        GameCommand command = redoStack.pop();
        if (!command.execute()) {
            lastFailureReason = command.getFailureReason();
            return false;
        }
        undoStack.push(command);
        lastFailureReason = "";
        return true;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }
}
