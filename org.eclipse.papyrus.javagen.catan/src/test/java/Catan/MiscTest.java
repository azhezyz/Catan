package Catan;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MiscTest {
    @Test
    void enumsAndActionDecision() {
        for (ResourceType rt : ResourceType.values()) {
            assertNotNull(rt.name());
        }
        for (BuildAction ba : BuildAction.values()) {
            assertNotNull(ba.name());
        }
        ActionDecision a1 = ActionDecision.none();
        assertEquals(BuildAction.NONE, a1.getAction());
        ActionDecision a2 = ActionDecision.road(7);
        assertEquals(BuildAction.ROAD, a2.getAction());
        assertEquals(7, a2.getTargetId());
    }

    @Test
    void buildingFactoryAndValidation() {
        Player p = new Player("tester");
        Building empty = Building.empty();
        assertFalse(empty.isOccupied());
        assertEquals(BuildingType.NONE, empty.getType());
        assertTrue(empty.getOwner().isEmpty());

        Building sett = Building.settlement(p);
        assertTrue(sett.isOccupied());
        assertEquals(BuildingType.SETTLEMENT, sett.getType());
        assertTrue(sett.isOwnedBy(p));
        assertEquals(1, sett.productionYield());

        Building city = Building.city(p);
        assertEquals(2, city.productionYield());

        // private constructor invariants already enforced via factories, cannot call directly
    }

    @Test
    void identifiableInterfaceAndSimple() {
        Tile t = new Tile(9, null, 0, Set.of(1));
        assertEquals(9, t.getId());
        assertTrue(((Identifiable)t).getId() == 9);
    }

    @Test
    void gameConfigAndState() {
        // config file loading where path nonexistent returns default
        GameConfig cfg = GameConfig.load(java.nio.file.Path.of("nonexistent.properties"));
        assertEquals(10, cfg.getMaxRounds());

        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1),
                List.of(new Path(0,0,1))
        );
        GameState state = new GameState(board);
        assertSame(board, state.getBoard());
    }

    @Test
    void gameEngineAndReport() {
        // minimal board with one player for engine
        Node n0 = new Node(0, Set.of(0), Set.of(1));
        Node n1 = new Node(1, Set.of(0), Set.of(0));
        Board board = new Board(
                List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0))),
                List.of(n0, n1),
                List.of(new Path(0,0,1))
        );
        Player p = new Player("alpha");
        GameEngine engine = new GameEngine(board, List.of(p));
        assertTrue(engine.rollDice() >= 2 && engine.rollDice() <= 12);
        SimulationReport r = engine.runSimulation(1);
        assertNotNull(r.getLogLines());
        assertTrue(r.getPlayers().contains(p));
    }

    @Test
    void demonstratorDoesSomething() {
        Demonstrator demo = new Demonstrator();
        assertNotNull(demo.toString());
    }
}
