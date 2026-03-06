package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Catan.Board;
import Catan.GameState;
import Catan.Node;
import Catan.Path;
import Catan.ResourceType;
import Catan.Tile;

class GameStateTest {

    @Test
    void exposesSameBoardReference() {
        Board board = new Board(
            List.of(new Tile(0, ResourceType.WOOD, 5, Set.of(0, 1))),
            List.of(new Node(0, Set.of(0), Set.of(1)), new Node(1, Set.of(0), Set.of(0))),
            List.of(new Path(0, 0, 1))
        );

        GameState state = new GameState(board);
        assertSame(board, state.getBoard());
    }
}
