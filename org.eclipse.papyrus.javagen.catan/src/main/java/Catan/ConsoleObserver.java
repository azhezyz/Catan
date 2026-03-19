package Catan;

import java.io.PrintStream;
import java.util.Objects;

public final class ConsoleObserver implements GameObserver {
    private final PrintStream out;

    public ConsoleObserver(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.TurnStartEvent turnStartEvent) {
            out.printf("Turn %d - %s (commands: Roll, List, Actions, Build ..., Undo, Redo, Go)%n",
                    turnStartEvent.turnId(), turnStartEvent.playerName());
            return;
        }
        if (event instanceof GameEvent.ActionEvent actionEvent) {
            out.printf("[%d] / [%s]: %s%n", actionEvent.turnId(), actionEvent.playerId(), actionEvent.action());
            return;
        }
        if (event instanceof GameEvent.GameEndedEvent gameEndedEvent) {
            if (gameEndedEvent.reason() == GameEvent.GameEndReason.INPUT_CLOSED) {
                out.println("Input closed. Game ended.");
            } else if (gameEndedEvent.reason() == GameEvent.GameEndReason.MAX_TURNS) {
                out.println("Reached max turns without a winner.");
            }
        }
    }
}
