package Catan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuildPlannerTest {
    @Test
    void costAccessors() {
        assertFalse(BuildPlanner.settlementCost().isEmpty());
        assertFalse(BuildPlanner.roadCost().isEmpty());
        assertFalse(BuildPlanner.cityCost().isEmpty());
    }

    @Test
    void noAffordableActionsWhenPoor() {
        Player p = new Player("x");
        // board must include at least one path; create minimal valid board
        Node a = new Node(0, Set.of(0), Set.of(1));
        Node b = new Node(1, Set.of(0), Set.of(0));
        Path dummy = new Path(0, 0, 1);
        Board empty = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(a, b),
                List.of(dummy));
        List<ActionDecision> list = BuildPlanner.availableActions(empty, p);
        assertTrue(list.isEmpty());
        ActionDecision decision = BuildPlanner.forcedDecision(empty, p);
        assertEquals(BuildAction.NONE, decision.getAction(), "expected no-action decision");
    }

    @Test
    void someActionsWhenResourcesAndBoard() {
        Player p = new Player("x");
        p.addResource(ResourceType.WOOD, 10);
        p.addResource(ResourceType.BRICK, 10);
        p.addResource(ResourceType.SHEEP, 10);
        p.addResource(ResourceType.WHEAT, 10);
        p.addResource(ResourceType.ORE, 10);

        Node n0 = new Node(0, Set.of(0), Set.of());
        Node n1 = new Node(1, Set.of(0), Set.of());
        Path path = new Path(0, 0, 1);
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1),
                List.of(path));

        // nothing claimed yet -> no actions
        assertTrue(BuildPlanner.availableActions(board, p).isEmpty());

        // give p settlement on n0 so new actions appear
        n0.claim(p);
        p.addSettlement(0);
        List<ActionDecision> actions = BuildPlanner.availableActions(board, p);
        assertFalse(actions.isEmpty());
        assertTrue(BuildPlanner.forcedDecision(board, p) != ActionDecision.none());
    }
}