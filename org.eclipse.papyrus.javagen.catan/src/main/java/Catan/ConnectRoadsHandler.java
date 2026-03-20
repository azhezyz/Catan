package Catan;
import java.util.List;

public class ConnectRoadsHandler extends BotRuleHandler {
    @Override
    protected ActionDecision evaluate(Board board, Player player, List<Player> allPlayers) {
        for (ActionDecision action : BuildPlanner.availableActions(board, player)) {
            if (action.getAction() == BuildAction.ROAD) {
                Path candidate = board.getPath(action.getTargetId());
                // R3.3: If this path bridges two existing parts of the player's network, buy it
                if (isNodeConnected(board, player, candidate.getNodeAId()) &&
                        isNodeConnected(board, player, candidate.getNodeBId())) {
                    return action;
                }
            }
        }
        return ActionDecision.none();
    }

    private boolean isNodeConnected(Board board, Player player, int nodeId) {
        if (board.getNode(nodeId).isOwnedBy(player)) return true;
        for (Path p : board.pathsAdjacentToNode(nodeId)) {
            if (p.isOwnedBy(player)) return true;
        }
        return false;
    }
}