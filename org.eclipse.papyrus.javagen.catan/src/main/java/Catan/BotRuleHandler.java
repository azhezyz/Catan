package Catan;

import java.util.List;

/**
 * Base class for the Chain of Responsibility pattern used in Machine Intelligence.
 */
public abstract class BotRuleHandler {
    private BotRuleHandler next;

    public void setNext(BotRuleHandler next) {
        this.next = next;
    }

    public ActionDecision handle(Board board, Player player, List<Player> allPlayers) {
        // Try to find a move using the current rule
        ActionDecision decision = evaluate(board, player, allPlayers);

        // If this rule found a valid action, return it immediately
        if (decision != null && decision.getAction() != BuildAction.NONE) {
            return decision;
        }

        // Otherwise, pass the request to the next rule in the chain
        if (next != null) {
            return next.handle(board, player, allPlayers);
        }

        // Fallback if no rules match
        return ActionDecision.none();
    }

    // Each specific rule implements its own evaluation logic here
    protected abstract ActionDecision evaluate(Board board, Player player, List<Player> allPlayers);
}