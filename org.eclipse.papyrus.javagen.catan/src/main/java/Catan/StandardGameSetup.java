package Catan;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StandardGameSetup {
    private StandardGameSetup() {
    }

    public static Board buildFullBoard() {
        List<Tile> tiles = List.of(
                new Tile(0, ResourceType.WOOD, 10, Set.of(0, 1, 2, 3, 4, 5)),
                new Tile(1, ResourceType.WHEAT, 11, Set.of(2, 1, 6, 7, 8, 9)),
                new Tile(2, ResourceType.BRICK, 8, Set.of(3, 2, 9, 10, 11, 12)),
                new Tile(3, ResourceType.ORE, 3, Set.of(4, 3, 12, 13, 14, 15)),
                new Tile(4, ResourceType.SHEEP, 11, Set.of(4, 5, 15, 16, 17, 18)),
                new Tile(5, ResourceType.SHEEP, 5, Set.of(0, 5, 16, 19, 20, 21)),
                new Tile(6, ResourceType.SHEEP, 12, Set.of(0, 1, 6, 20, 22, 23)),
                new Tile(7, ResourceType.WHEAT, 3, Set.of(7, 8, 24, 25, 26, 27)),
                new Tile(8, ResourceType.ORE, 6, Set.of(8, 9, 10, 27, 28, 29)),
                new Tile(9, ResourceType.WOOD, 4, Set.of(10, 11, 29, 30, 31, 32)),
                new Tile(10, ResourceType.ORE, 6, Set.of(11, 12, 13, 32, 33, 34)),
                new Tile(11, ResourceType.WHEAT, 9, Set.of(13, 14, 34, 35, 36, 37)),
                new Tile(12, ResourceType.WOOD, 5, Set.of(14, 15, 17, 37, 38, 39)),
                new Tile(13, ResourceType.BRICK, 9, Set.of(17, 18, 39, 40, 41, 42)),
                new Tile(14, ResourceType.BRICK, 8, Set.of(16, 18, 21, 40, 43, 44)),
                new Tile(15, ResourceType.WHEAT, 4, Set.of(19, 21, 43, 45, 46, 47)),
                new Tile(16, null, 0, Set.of(19, 20, 22, 46, 48, 49)),
                new Tile(17, ResourceType.WOOD, 2, Set.of(22, 23, 49, 50, 51, 52)),
                new Tile(18, ResourceType.SHEEP, 10, Set.of(6, 7, 23, 24, 52, 53))
        );

        List<Node> nodes = List.of(
                new Node(0, Set.of(0, 5, 6), Set.of(1, 5, 20)),
                new Node(1, Set.of(0, 1, 6), Set.of(0, 2, 6)),
                new Node(2, Set.of(2, 0, 1), Set.of(1, 3, 9)),
                new Node(3, Set.of(0, 3, 2), Set.of(12, 4, 2)),
                new Node(4, Set.of(3, 4, 0), Set.of(3, 5, 15)),
                new Node(5, Set.of(0, 5, 4), Set.of(0, 4, 16)),
                new Node(6, Set.of(1, 6, 18), Set.of(1, 7, 23)),
                new Node(7, Set.of(1, 7, 18), Set.of(6, 8, 24)),
                new Node(8, Set.of(1, 7, 8), Set.of(7, 9, 27)),
                new Node(9, Set.of(1, 2, 8), Set.of(2, 8, 10)),
                new Node(10, Set.of(2, 8, 9), Set.of(9, 11, 29)),
                new Node(11, Set.of(2, 9, 10), Set.of(10, 12, 32)),
                new Node(12, Set.of(2, 3, 10), Set.of(3, 11, 13)),
                new Node(13, Set.of(3, 10, 11), Set.of(14, 34, 12)),
                new Node(14, Set.of(3, 12, 11), Set.of(13, 15, 37)),
                new Node(15, Set.of(3, 4, 12), Set.of(17, 14, 4)),
                new Node(16, Set.of(4, 5, 14), Set.of(5, 18, 21)),
                new Node(17, Set.of(4, 13, 12), Set.of(15, 39, 18)),
                new Node(18, Set.of(4, 14, 13), Set.of(17, 16, 40)),
                new Node(19, Set.of(5, 16, 15), Set.of(21, 20, 46)),
                new Node(20, Set.of(5, 6, 16), Set.of(0, 22, 19)),
                new Node(21, Set.of(5, 14, 15), Set.of(16, 43, 19)),
                new Node(22, Set.of(6, 16, 17), Set.of(20, 23, 49)),
                new Node(23, Set.of(6, 17, 18), Set.of(6, 22, 52)),
                new Node(24, Set.of(7, 18), Set.of(7, 25, 53)),
                new Node(25, Set.of(7), Set.of(24, 26)),
                new Node(26, Set.of(7), Set.of(25, 27)),
                new Node(27, Set.of(7, 8), Set.of(8, 26, 28)),
                new Node(28, Set.of(8), Set.of(27, 29)),
                new Node(29, Set.of(8, 9), Set.of(10, 28, 30)),
                new Node(30, Set.of(9), Set.of(29, 31)),
                new Node(31, Set.of(9), Set.of(30, 32)),
                new Node(32, Set.of(9, 10), Set.of(11, 31, 33)),
                new Node(33, Set.of(10), Set.of(32, 34)),
                new Node(34, Set.of(10, 11), Set.of(13, 33, 35)),
                new Node(35, Set.of(11), Set.of(36, 34)),
                new Node(36, Set.of(11), Set.of(35, 37)),
                new Node(37, Set.of(11,12), Set.of(38, 36,14)),
                new Node(38, Set.of(12), Set.of(39, 37)),
                new Node(39, Set.of(12,13), Set.of(41, 38, 17)),
                new Node(40, Set.of(13,14), Set.of(44, 42, 18)),
                new Node(41, Set.of(13), Set.of(39,42)),
                new Node(42, Set.of(13), Set.of(41, 40)),
                new Node(43, Set.of(14,15), Set.of(21,45, 44)),
                new Node(44, Set.of(14), Set.of(43, 40)),
                new Node(45, Set.of(15), Set.of(43, 47)),
                new Node(46, Set.of(15,16), Set.of(19,47,48)),
                new Node(47, Set.of(15), Set.of(46, 45)),
                new Node(48, Set.of(16), Set.of(46,49)),
                new Node(49, Set.of(16, 17), Set.of(22, 48, 50)),
                new Node(50, Set.of(17), Set.of(49, 51)),
                new Node(51, Set.of(17), Set.of(50, 52)),
                new Node(52, Set.of(17, 18), Set.of(23, 51, 53)),
                new Node(53, Set.of(18), Set.of(24, 52))
        );

        List<Path> paths = List.of(
                new Path(0, 4, 5), new Path(1, 5, 0), new Path(2, 0, 1), new Path(3, 1, 2),
                new Path(4, 2, 3), new Path(5, 1, 6), new Path(6, 6, 7), new Path(7, 7, 8),
                new Path(8, 8, 9), new Path(9, 9, 2), new Path(10, 3, 12), new Path(11, 12, 11),
                new Path(12, 11, 10), new Path(13, 10, 9), new Path(14, 4, 15), new Path(15, 15, 14),
                new Path(16, 14, 13), new Path(17, 13, 12), new Path(18, 15, 17), new Path(19, 17, 18),
                new Path(20, 18, 16), new Path(21, 16, 5), new Path(22, 16, 21), new Path(23, 21, 19),
                new Path(24, 19, 20), new Path(25, 20, 0),
                new Path(26, 20, 22), new Path(27, 22, 23), new Path(28, 23, 6),
                new Path(29, 7, 24), new Path(30, 24, 25), new Path(31, 25, 26), new Path(32, 26, 27), new Path(33, 27, 8),
                new Path(34, 27, 28), new Path(35, 28, 29), new Path(36, 29, 10),
                new Path(37, 29, 30), new Path(38, 30, 31), new Path(39, 31, 32), new Path(40, 32, 11),
                new Path(41, 32, 33), new Path(42, 33, 34), new Path(43, 34, 13),
                new Path(44, 34, 35), new Path(45, 35, 36), new Path(46, 36, 37), new Path(47, 37, 14),
                new Path(48, 37, 38), new Path(49, 38, 39), new Path(50, 39, 17),
                new Path(51, 39, 41), new Path(52, 41, 42), new Path(53, 42, 40), new Path(54, 40, 18),
                new Path(55, 40, 44), new Path(56, 44, 43), new Path(57, 43, 21),
                new Path(58, 43, 45), new Path(59, 45, 47), new Path(60, 47, 46), new Path(61, 46, 19),
                new Path(62, 46, 48), new Path(63, 48, 49), new Path(64, 49, 22),
                new Path(65, 49, 50), new Path(66, 50, 51), new Path(67, 51, 52), new Path(68, 52, 23),
                new Path(69, 52, 53), new Path(70, 53, 24), new Path(71, 3, 4)
        );

        return new Board(tiles, nodes, paths);
    }

    public static void seedInitialState(Board board, Player alice, Player bob, Player charlie, Player diana) {
        claimSettlement(board, alice, 18);
        claimRoadByNodes(board, alice, 18, 16);
        claimSettlement(board, alice, 13);
        claimRoadByNodes(board, alice, 13, 12);

        claimSettlement(board, bob, 11);
        claimRoadByNodes(board, bob, 11, 10);
        claimSettlement(board, bob, 7);
        claimRoadByNodes(board, bob, 7, 6);

        claimSettlement(board, charlie, 19);
        claimRoadByNodes(board, charlie, 19, 21);
        claimSettlement(board, charlie, 9);
        claimRoadByNodes(board, charlie, 9, 8);

        claimSettlement(board, diana, 15);
        claimRoadByNodes(board, diana, 15, 14);
        claimSettlement(board, diana, 23);
        claimRoadByNodes(board, diana, 23, 22);

        for (Player player : List.of(alice, bob, charlie, diana)) {
            player.addResource(ResourceType.WOOD, 1);
            player.addResource(ResourceType.BRICK, 1);
            player.addResource(ResourceType.SHEEP, 1);
            player.addResource(ResourceType.WHEAT, 1);
        }
    }

    private static void claimSettlement(Board board, Player player, int nodeId) {
        Node node = board.getNode(nodeId);
        node.claim(player);
        player.addSettlement(nodeId);
    }

    private static void claimRoadByNodes(Board board, Player player, int nodeA, int nodeB) {
        Integer directPathId = findPathId(board, nodeA, nodeB);
        if (directPathId != null) {
            Path path = board.getPath(directPathId);
            path.claim(player);
            player.addRoad(path.getId());
            return;
        }

        Integer fallbackPathId = findFallbackPathId(board, nodeA, nodeB);
        if (fallbackPathId != null) {
            Path path = board.getPath(fallbackPathId);
            path.claim(player);
            player.addRoad(path.getId());
            return;
        }

        throw new IllegalArgumentException("No road placement found for nodes " + nodeA + " and " + nodeB);
    }

    private static Integer findPathId(Board board, int nodeA, int nodeB) {
        for (Path path : board.getPaths()) {
            boolean direct = path.getNodeAId() == nodeA && path.getNodeBId() == nodeB;
            boolean reverse = path.getNodeAId() == nodeB && path.getNodeBId() == nodeA;
            if (direct || reverse) {
                return path.getId();
            }
        }
        return null;
    }

    private static Integer findFallbackPathId(Board board, int fromNode, int targetNode) {
        Map<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(fromNode);
        parent.put(fromNode, -1);

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            if (current == targetNode) {
                break;
            }
            for (Path edge : board.pathsAdjacentToNode(current)) {
                int next = edge.getNodeAId() == current ? edge.getNodeBId() : edge.getNodeAId();
                if (!parent.containsKey(next)) {
                    parent.put(next, current);
                    queue.addLast(next);
                }
            }
        }

        if (!parent.containsKey(targetNode)) {
            return null;
        }

        int step = targetNode;
        while (parent.get(step) != fromNode && parent.get(step) != -1) {
            step = parent.get(step);
        }
        if (parent.get(step) == -1) {
            return null;
        }
        return findPathId(board, fromNode, step);
    }
}
