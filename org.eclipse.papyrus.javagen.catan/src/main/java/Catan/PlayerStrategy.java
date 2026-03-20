package Catan;

import java.util.List;
import java.util.Scanner;

public interface PlayerStrategy {
    // Returns the next command string (e.g., "roll", "build road 1", "go")
    String getNextCommand(Board board, Player player, List<Player> allPlayers, Scanner scanner);
}