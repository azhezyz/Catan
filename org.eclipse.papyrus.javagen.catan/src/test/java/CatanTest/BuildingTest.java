package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import Catan.Building;
import Catan.BuildingType;

class BuildingTest {

    @Test
    void emptyBuilding_hasNoneTypeAndNoYield() {
        Building b = Building.empty();

        assertEquals(BuildingType.NONE, b.getType());
        assertFalse(b.isOccupied());
        assertEquals(0, b.productionYield());
    }

    @Test
    void settlementBuilding_hasSettlementTypeAndYieldOne() {
        Building b = Building.settlement();

        assertEquals(BuildingType.SETTLEMENT, b.getType());
        assertTrue(b.isOccupied());
        assertEquals(1, b.productionYield());
    }

    @Test
    void cityBuilding_hasCityTypeAndYieldTwo() {
        Building b = Building.city();

        assertEquals(BuildingType.CITY, b.getType());
        assertTrue(b.isOccupied());
        assertEquals(2, b.productionYield());
    }

    @Test
    void flyweights_areSharedInstances() {
        assertSame(Building.empty(), Building.empty());
        assertSame(Building.settlement(), Building.settlement());
        assertSame(Building.city(), Building.city());
    }

    @Test
    void ofReturnsSameFlyweightForEachType() {
        assertSame(Building.empty(), Building.of(BuildingType.NONE));
        assertSame(Building.settlement(), Building.of(BuildingType.SETTLEMENT));
        assertSame(Building.city(), Building.of(BuildingType.CITY));
    }
}
