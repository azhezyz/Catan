package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import Catan.Player;
import Catan.ResourceType;

class PlayerTest {

    private Player player;

    // create a instance every test to avoid interfering
    @BeforeEach
    void setup() {
        player = new Player("Alice");
    }
    //Test the initialization of the player
    @Test
    void testPlayerInitialization() {
        assertEquals("Alice", player.getName());
        assertEquals(0, player.getTotalResourceCount());
        assertEquals(0, player.getSettlementNodeIds().size());
        assertEquals(0, player.getCityNodeIds().size());
        assertEquals(0, player.getRoadPathIds().size());
    }
    //Test if the resource can be added and got correctly with the amount
    @Test
    void testAddResource() {
        player.addResource(ResourceType.BRICK, 2);
        player.addResource(ResourceType.WOOD, 1);
        assertEquals(2, player.getResourceCount(ResourceType.BRICK));
        assertEquals(1, player.getResourceCount(ResourceType.WOOD));
        assertEquals(3, player.getTotalResourceCount());
    }
    //Test if the player can afford a specific amount of cost and check the left resources after spending
    @Test
    void testCanAffordAndSpend() {
        player.addResource(ResourceType.BRICK, 2);
        player.addResource(ResourceType.WOOD, 2);
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.BRICK, 1);
        cost.put(ResourceType.WOOD, 1);
        // Player should be able to afford this cost
        assertTrue(player.canAfford(cost));
        // Spend resources
        player.spend(cost);
        // Verify remaining resources
        assertEquals(1, player.getResourceCount(ResourceType.BRICK));
        assertEquals(1, player.getResourceCount(ResourceType.WOOD));
    }
    //Test if a settlement can be added to a player and it can be upgraded to a city
    @Test
    void testAddSettlementAndCity() {
        player.addSettlement(5);
        assertTrue(player.getSettlementNodeIds().contains(5));
        assertEquals(1, player.getSettlementNodeIds().size());
        // Upgrade the settlement to a city
        player.addCity(5);
        assertFalse(player.getSettlementNodeIds().contains(5));
        assertTrue(player.getCityNodeIds().contains(5));
    }
    //Test if a road can be added to a player
    @Test
    void testAddRoad() {
        player.addRoad(10);
        player.addRoad(11);
        assertTrue(player.getRoadPathIds().contains(10));
        assertTrue(player.getRoadPathIds().contains(11));
        assertEquals(2, player.getRoadPathIds().size());
    }
    //Test the if the VP can be correctly calculated based on settlements
    @Test
    void testVictoryPointsSettlement() {
        player.addSettlement(1);
        player.addSettlement(2);

        assertEquals(2, player.getVictoryPoints());
    }
    @Test
    void testVictoryPointsCity() {
        player.addSettlement(1);
        player.addCity(1);
        assertEquals(2, player.getVictoryPoints());
    }
    @Test
    void testVictoryPointsLongestRoad() {
        player.addSettlement(1);
        player.setHasLongestRoad(true);

        assertEquals(3, player.getVictoryPoints()); // 1 settlement + 2 bonus
    }
    @Test
    void testCannotSpendWithoutResources() {
        Map<ResourceType, Integer> cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.BRICK, 1);
        assertThrows(IllegalStateException.class, () -> player.spend(cost));
    }
    @Test
    void testDuplicateSettlementThrowsException() {
        player.addSettlement(3);
        assertThrows(IllegalStateException.class, () -> player.addSettlement(3));
    }
    @Test
    void testUpgradeWithoutSettlementFails() {
        assertThrows(IllegalStateException.class, () -> player.addCity(10));
    }
}
