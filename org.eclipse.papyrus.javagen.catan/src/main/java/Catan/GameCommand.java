package Catan;

public interface GameCommand {
    boolean execute();

    void undo();

    String getFailureReason();
}
