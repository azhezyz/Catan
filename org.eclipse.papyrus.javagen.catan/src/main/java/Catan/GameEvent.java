package Catan;

import java.util.Map;

public interface GameEvent {
    enum UndoRedoType {
        UNDO,
        REDO
    }

    enum GameEndReason {
        WIN,
        INPUT_CLOSED,
        MAX_TURNS
    }

    record TurnStartEvent(int turnId, String playerName) implements GameEvent {
    }

    record DiceRolledEvent(int turnId, String playerName, int roll) implements GameEvent {
    }

    record BuildSucceededEvent(int turnId, String playerName, BuildAction buildAction, String target)
            implements GameEvent {
    }

    record BuildFailedEvent(int turnId, String playerName, BuildAction buildAction, String target, String reason)
            implements GameEvent {
    }

    record UndoRedoEvent(int turnId, String playerName, UndoRedoType type, boolean success, String detail)
            implements GameEvent {
    }

    record RobberMovedEvent(int turnId, String rollerName, int tileId, String victimName) implements GameEvent {
    }

    record RobbedEvent(int turnId, String rollerName, String victimName, ResourceType resourceType)
            implements GameEvent {
    }

    record GameEndedEvent(GameEndReason reason, int turnId, String playerName, int victoryPoints, String detail)
            implements GameEvent {
    }

    record ActionEvent(int turnId, String playerId, String action) implements GameEvent {
    }

    record StateChangedEvent(int robberTileId, Board board, Map<Player, String> playerColors) implements GameEvent {
    }
}
