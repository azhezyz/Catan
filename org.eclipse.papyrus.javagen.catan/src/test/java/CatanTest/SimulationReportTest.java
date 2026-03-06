package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import Catan.Player;
import Catan.ResourceType;
import Catan.SimulationReport;

class SimulationReportTest {

    @Test
    void constructorCopiesInputCollections() {
        List<String> logs = new ArrayList<>(List.of("line1"));
        List<Player> players = new ArrayList<>(List.of(new Player("alice")));

        SimulationReport report = new SimulationReport(logs, players);

        logs.add("line2");
        players.add(new Player("bob"));

        assertEquals(1, report.getLogLines().size());
        assertEquals(1, report.getPlayers().size());
    }

    @Test
    void summarizeContainsKeyPlayerStats() {
        Player alice = new Player("alice");
        alice.addResource(ResourceType.WOOD, 2);
        alice.addSettlement(1);
        alice.addCity(1);
        alice.addRoad(10);
        alice.setHasLongestRoad(true);

        SimulationReport report = new SimulationReport(List.of("start"), List.of(alice));
        String summary = report.summarize();

        assertTrue(summary.contains("Final Summary"));
        assertTrue(summary.contains("alice scores 4"));
        assertTrue(summary.contains("WOOD=2"));
        assertTrue(summary.contains("settlements=0"));
        assertTrue(summary.contains("cities=1"));
        assertTrue(summary.contains("roads=1"));
    }
}
