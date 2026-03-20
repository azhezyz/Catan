package Catan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implements R3.2: Evaluates the benefit of applying a rule.
 * Highest immediate value wins. Ties are broken randomly.
 */
public final class ValueMaximizationHandler extends BotRuleHandler {
    private final Random random;

    public ValueMaximizationHandler(Random random) {
        this.random = random;
    }

    @Override
    protected ActionDecision evaluate(Board board, Player player, List<Player> allPlayers) {
        List<ActionDecision> available = BuildPlanner.availableActions(board, player);
        if (available.isEmpty()) {
            return ActionDecision.none();
        }

        double bestScore = -1.0;
        List<ActionDecision> ties = new ArrayList<>();

        for (ActionDecision action : available) {
            double score = calculateValue(action, player);

            if (score > bestScore) {
                bestScore = score;
                ties.clear();
                ties.add(action);
            } else if (Double.compare(score, bestScore) == 0) {
                ties.add(action);
            }
        }

        if (ties.isEmpty()) {
            return ActionDecision.none();
        }

        // R3.2: "In case of a tie... choose a random action."
        return ties.get(random.nextInt(ties.size()));
    }

    private double calculateValue(ActionDecision action, Player player) {
        double score = 0.0;

        Map<ResourceType, Integer> cost = switch (action.getAction()) {
            case SETTLEMENT -> BuildPlanner.settlementCost();
            case CITY -> BuildPlanner.cityCost();
            case ROAD -> BuildPlanner.roadCost();
            case NONE -> Map.of();
        };

        // R3.2: Earning a VP (Settlement or City) -> 1.0
        if (action.getAction() == BuildAction.SETTLEMENT || action.getAction() == BuildAction.CITY) {
            score = 1.0;
        }
        // R3.2: Building something without earning a VP (Road) -> 0.8
        else if (action.getAction() == BuildAction.ROAD) {
            score = 0.8;
        }

        // Calculate how many cards remain after spending
        int totalCost = 0;
        for (int amount : cost.values()) {
            totalCost += amount;
        }
        int cardsRemaining = player.getTotalResourceCount() - totalCost;

        // R3.2: Spending cards in a way that less than 5 cards remain: 0.5
        // (We use Math.max so we don't accidentally downgrade a 0.8 road to 0.5)
        if (cardsRemaining < 5 && totalCost > 0) {
            score = Math.max(score, 0.5);
        }

        return score;
    }
}