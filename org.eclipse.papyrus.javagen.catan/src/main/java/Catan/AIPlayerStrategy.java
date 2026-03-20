package Catan;

import java.util.List;
import java.util.Scanner;

public class AIPlayerStrategy implements PlayerStrategy {
    private final BotRuleHandler ruleChain;
    private boolean rolledThisTurn = false;

    public AIPlayerStrategy(BotRuleHandler ruleChain) {
        this.ruleChain = ruleChain;
    }

    @Override
    public String getNextCommand(Board board, Player player, List<Player> allPlayers, Scanner scanner) {
        // 1. Always roll the dice first
        if (!rolledThisTurn) {
            rolledThisTurn = true;
            System.out.println(player.getName() + " (AI) is rolling...");
            return "roll";
        }

        // 2. Ask the R3.2 / R3.3 Chain of Responsibility what to do
        ActionDecision move = ruleChain.handle(board, player, allPlayers);

        // 3. If no rules trigger (or can't afford anything), end the turn
        if (move.getAction() == BuildAction.NONE) {
            rolledThisTurn = false; // Reset for next turn
            System.out.println(player.getName() + " (AI) ends their turn.");
            return "go";
        }

        // 4. Translate the AI's decision into the text command your engine expects
        String command;
        if (move.getAction() == BuildAction.ROAD) {
            // Translate the Path ID back into NodeA and NodeB
            Path p = board.getPath(move.getTargetId());
            command = "build road " + p.getNodeAId() + ", " + p.getNodeBId();
        } else {
            // Settlements and Cities use the Node ID directly
            command = "build " + move.getAction().name().toLowerCase() + " " + move.getTargetId();
        }

        System.out.println(player.getName() + " (AI) decides to: " + command);
        return command;
    }
}
