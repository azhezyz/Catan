package Catan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {
    private Player p;

    @BeforeEach
    void init() {
        p = new Player("bob");
    }

    @Test
    void nameValidation() {
        assertEquals("bob", p.getName());
        assertThrows(NullPointerException.class, () -> new Player(null));
        assertThrows(IllegalArgumentException.class, () -> new Player("   "));
    }

    @Test
    void resourceManipulation() {
        assertEquals(0, p.getTotalResourceCount());
        p.addResource(ResourceType.WOOD, 3);
        assertEquals(3, p.getResourceCount(ResourceType.WOOD));
        assertThrows(IllegalArgumentException.class, () -> p.addResource(ResourceType.WOOD, 0));
        // null resource should throw NPE (requireNonNull in implementation)
        assertThrows(NullPointerException.class, () -> p.addResource(null, 1));

        Map<ResourceType, Integer> cost = Map.of(ResourceType.WOOD, 2);
        assertTrue(p.canAfford(cost));
        p.spend(cost);
        assertEquals(1, p.getResourceCount(ResourceType.WOOD));

        assertThrows(IllegalStateException.class, () -> p.spend(Map.of(ResourceType.WOOD, 10)));
    }

    @Test
    void buildingRoadTracking() {
        p.addSettlement(1);
        assertTrue(p.getSettlementNodeIds().contains(1));
        assertThrows(IllegalStateException.class, () -> p.addSettlement(1));
        p.addRoad(5);
        assertTrue(p.getRoadPathIds().contains(5));
        assertThrows(IllegalStateException.class, () -> p.addRoad(5));
        p.addCity(1);
        assertTrue(p.getCityNodeIds().contains(1));
        assertThrows(IllegalStateException.class, () -> p.addCity(2));
    }

    @Test
    void victoryPointsAndLongestRoad() {
        assertEquals(0, p.getVictoryPoints());
        p.addSettlement(1);
        assertEquals(1, p.getVictoryPoints());
        p.addCity(1); // upgrade adds 2 additional points and removes settlement
        assertEquals(2, p.getVictoryPoints());
        p.setHasLongestRoad(true);
        assertEquals(4, p.getVictoryPoints());
    }

    @Test
    void longestRoadComputationSimple() {
        // create simple board with three nodes in a chain and two paths
        // all nodes reference the same single tile id (0) to satisfy board checks
        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0,2));
        Node n2 = new Node(2, Set.of(0), Set.of(1));
        Path p0 = new Path(0, 0, 1);
        Path p1 = new Path(1, 1, 2);
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1, n2),
                List.of(p0, p1));
        // player must both record and claim roads for ownership checks
        p.addRoad(0);
        p0.claim(p);
        p.addRoad(1);
        p1.claim(p);
        assertEquals(2, p.calculateLongestRoad(board));

        // block the road by placing opponent building
        Player other = new Player("other");
        n1.claim(other);
        assertEquals(1, p.calculateLongestRoad(board));
    }
}