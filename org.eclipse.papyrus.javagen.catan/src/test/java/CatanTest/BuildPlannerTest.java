package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.Board;
import Catan.BuildAction;
import Catan.BuildPlanner;
import Catan.Player;
import Catan.ResourceType;
import Catan.Tile;
import Catan.Node;
import Catan.Path;

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

    @Test
    void forcedDecisionPrefersCityBeforeOtherActions() {
        Player p = new Player("x");
        p.addResource(ResourceType.WOOD, 10);
        p.addResource(ResourceType.BRICK, 10);
        p.addResource(ResourceType.SHEEP, 10);
        p.addResource(ResourceType.WHEAT, 10);
        p.addResource(ResourceType.ORE, 10);

        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Path path = new Path(0, 0, 1);
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0, 1))),
                List.of(n0, n1),
                List.of(path));

        n0.claim(p);
        p.addSettlement(0);
        ActionDecision first = BuildPlanner.forcedDecision(board, p);
        assertEquals(BuildAction.CITY, first.getAction());
        assertEquals(0, first.getTargetId());
    }

    @Test
    void onlyRoadActionWhenOnlyRoadAffordable() {
        Player p = new Player("x");
        p.addResource(ResourceType.WOOD, 1);
        p.addResource(ResourceType.BRICK, 1);

        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Path roadToBuild = new Path(0, 0, 1);
        Path ownedRoad = new Path(1, 0, 2);
        Node n2 = new Node(2, Set.of(0), Set.of(0));
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0, 1, 2))),
                List.of(n0, n1, n2),
                List.of(roadToBuild, ownedRoad));

        ownedRoad.claim(p);
        p.addRoad(1);

        List<ActionDecision> actions = BuildPlanner.availableActions(board, p);
        assertEquals(1, actions.size());
        assertEquals(BuildAction.ROAD, actions.get(0).getAction());
        assertEquals(0, actions.get(0).getTargetId());
    }

    @Test
    void cannotUpgradeToCityWithoutCityCost() {
        Player p = new Player("x");
        p.addResource(ResourceType.WOOD, 10);
        p.addResource(ResourceType.BRICK, 10);
        p.addResource(ResourceType.SHEEP, 10);
        p.addResource(ResourceType.WHEAT, 1);
        p.addResource(ResourceType.ORE, 2);

        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Path path = new Path(0, 0, 1);
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0, 1))),
                List.of(n0, n1),
                List.of(path));

        n0.claim(p);
        p.addSettlement(0);
        List<ActionDecision> actions = BuildPlanner.availableActions(board, p);
        assertTrue(actions.stream().noneMatch(a -> a.getAction() == BuildAction.CITY));
    }

}
