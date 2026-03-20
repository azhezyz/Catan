package Catan;
import java.util.List;

public class OverSevenCardsHandler extends BotRuleHandler {
    @Override
    protected ActionDecision evaluate(Board board, Player player, List<Player> allPlayers) {
        if (player.getTotalResourceCount() > 7) {
            // R3.3: Force the agent to spend resources
            ActionDecision decision = BuildPlanner.forcedDecision(board, player);
            if (decision.getAction() != BuildAction.NONE) {
                return decision;
            }
        }
        return ActionDecision.none();
    }
}
