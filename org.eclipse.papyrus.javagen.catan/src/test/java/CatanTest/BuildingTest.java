package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import Catan.Player;
import Catan.Building;
import Catan.BuildingType;

class BuildingTest {

	// Set mock players used for test
    private Player mockPlayerA() {
        return new Player("Alice");
    }

    private Player mockPlayerB() {
        return new Player("Bob");
    }

    
    // Test empty building type with no owner
    @Test
    void emptyBuilding_withNoneTypeAndNoOwner() {
        Building b = Building.empty();

        assertEquals(BuildingType.NONE, b.getType());
        assertTrue(b.getOwner().isEmpty());
        assertFalse(b.isOccupied());
        assertEquals(0, b.productionYield());
    }

    // Test settlement constructed by user
    @Test
    void settlementBuilding_withCorrectTypeAndOwner() {
        Player p = mockPlayerA();
        Building b = Building.settlement(p);

        assertEquals(BuildingType.SETTLEMENT, b.getType());
        assertEquals(Optional.of(p), b.getOwner());
        assertTrue(b.isOccupied());
        assertEquals(1, b.productionYield());
    }

 // Test city constructed by user
    @Test
    void cityBuilding_withCorrectTypeAndOwner() {
        Player p = mockPlayerA();
        Building b = Building.city(p);

        assertEquals(BuildingType.CITY, b.getType());
        assertEquals(Optional.of(p), b.getOwner());
        assertTrue(b.isOccupied());
        assertEquals(2, b.productionYield());
    }


    // Test the owned by method
    @Test
    void isOwnedBy_returnsTrueForCorrectOwner() {
        Player p = mockPlayerA();
        Building b = Building.city(p);

        assertTrue(b.isOwnedBy(p));
    }

    @Test
    void isOwnedBy_returnsFalseForDifferentOwner() {
        Player p1 = mockPlayerA();
        Player p2 = mockPlayerB();
        Building b = Building.settlement(p1);

        assertFalse(b.isOwnedBy(p2));
    }

    @Test
    void isOwnedBy_returnsFalseForEmptyBuilding() {
        Player p = mockPlayerA();
        Building b = Building.empty();

        assertFalse(b.isOwnedBy(p));
    }
}