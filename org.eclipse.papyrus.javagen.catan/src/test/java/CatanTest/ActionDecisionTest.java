package CatanTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Catan.ActionDecision;
import Catan.BuildAction;

class ActionDecisionTest {

    @Test
    void testNoneDecision() {
		// Verify that a 'Pass' action correctly defaults to an ID of -1
        ActionDecision decision = ActionDecision.none();
        assertEquals(BuildAction.NONE, decision.getAction(), "Action should be NONE");
        assertEquals(-1, decision.getTargetId(), "Target ID for NONE should be -1");
    }

    @Test
    void testRoadDecision() {
		// Verify the Road factory correctly maps the pathId
        int pathId = 42;
        ActionDecision decision = ActionDecision.road(pathId);
        assertEquals(BuildAction.ROAD, decision.getAction(), "Action should be ROAD");
        assertEquals(pathId, decision.getTargetId(), "Target ID should match the provided pathId");
    }

    @Test
    void testSettlementDecision() {
		// Verify the Settlement factory correctly maps the nodeId
        int nodeId = 15;
        ActionDecision decision = ActionDecision.settlement(nodeId);
        assertEquals(BuildAction.SETTLEMENT, decision.getAction(), "Action should be SETTLEMENT");
        assertEquals(nodeId, decision.getTargetId(), "Target ID should match the provided nodeId");
    }

    @Test
    void testCityDecision() {
		// Verify the City upgrade factory correctly maps the nodeId
        int nodeId = 7;
        ActionDecision decision = ActionDecision.city(nodeId);
        assertEquals(BuildAction.CITY, decision.getAction(), "Action should be CITY");
        assertEquals(nodeId, decision.getTargetId(), "Target ID should match the provided nodeId");
    }
}
