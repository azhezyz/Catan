package Catan;
import java.util.List;

public class DefendLongestRoadHandler extends BotRuleHandler {
    @Override
    protected ActionDecision evaluate(Board board, Player player, List<Player> allPlayers) {
        int myRoad = player.calculateLongestRoad(board);
        int maxOpponent = 0;
        for (Player p : allPlayers) {
            if (p != player) maxOpponent = Math.max(maxOpponent, p.calculateLongestRoad(board));
        }

        // R3.3: If opponent is at most 1 road shorter, buy a connected road segment
        if (myRoad >= 4 && maxOpponent >= myRoad - 1) {
            for (ActionDecision action : BuildPlanner.availableActions(board, player)) {
                if (action.getAction() == BuildAction.ROAD) return action;
            }
        }
        return ActionDecision.none();
    }
}
