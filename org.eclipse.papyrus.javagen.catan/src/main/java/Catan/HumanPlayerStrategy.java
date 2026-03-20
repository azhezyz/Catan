package Catan;

import java.util.List;
import java.util.Scanner;

public class HumanPlayerStrategy implements PlayerStrategy {
    @Override
    public String getNextCommand(Board board, Player player, List<Player> allPlayers, Scanner scanner) {
        // Only block and wait for input if it's a human!
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }
}