package CatanTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Catan.Tile;
import Catan.ResourceType;
import java.util.Set;

class TileTest {

    @Test
    void testValidResourceTile() {
        // Standard setup: Ore tile with number token 8
        Tile tile = new Tile(1, ResourceType.ORE, 8, Set.of(1, 2, 3, 4, 5, 6));
        assertEquals(1, tile.getId());
        assertEquals(ResourceType.ORE, tile.getResourceType(), "Resource type should be ORE");
        assertEquals(8, tile.getNumberToken(), "Token should be 8");
    }

    @Test
    void testValidDesertTile() {
        // Desert setup: No resource, token must be 0
        Tile desert = new Tile(0, null, 0, Set.of(10, 11, 12));
        assertNull(desert.getResourceType(), "Desert should have null resource");
        assertEquals(0, desert.getNumberToken(), "Desert token must be 0");
    }

    @Test
    void testInvalidNumberTokens() {
        // Tokens for resource tiles must be 2-12
        assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, ResourceType.BRICK, 13, Set.of(1, 2, 3)), 
            "Should fail for token > 12");
            
        assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, ResourceType.BRICK, 1, Set.of(1, 2, 3)), 
            "Should fail for token < 2");
    }

    @Test
    void testDesertMismatch() {
        // Should fail if Desert has a non-zero token
        assertThrows(IllegalArgumentException.class, () -> 
            new Tile(5, null, 6, Set.of(1, 2)), 
            "Desert cannot have a production token");
    }

    @Test
    void testEmptyNodesThrowsException() {
        // A tile must be connected to at least one node to exist
        assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, ResourceType.WOOD, 4, Set.of()), 
            "Tile must have adjacent nodes");
    }
}