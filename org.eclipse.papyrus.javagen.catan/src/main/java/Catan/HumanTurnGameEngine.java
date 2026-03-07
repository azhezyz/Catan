package Catan;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntSupplier;

public final class HumanTurnGameEngine {
    private static final List<String> COLOR_ORDER = List.of("RED", "BLUE", "ORANGE", "WHITE");

    private final Board board;
    private final List<Player> players;
    private final Scanner scanner;
    private final PrintStream out;
    private final java.nio.file.Path statePath;
    private final Map<Player, String> playerColors;
    private final IntSupplier rollSupplier;
    private final Random random;
    private int robberTileId;

    private Player longestRoadHolder = null;
    private int currentMaxRoadLength = 4;

    public HumanTurnGameEngine(
            Board board,
            List<Player> players,
            Scanner scanner,
            PrintStream out,
            java.nio.file.Path statePath
    ) {
        this(board, players, scanner, out, statePath, new DiceSet()::nextRoll, new Random());
    }

    public HumanTurnGameEngine(
            Board board,
            List<Player> players,
            Scanner scanner,
            PrintStream out,
            java.nio.file.Path statePath,
            IntSupplier rollSupplier,
            Random random
    ) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = List.copyOf(Objects.requireNonNull(players, "players"));
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.out = Objects.requireNonNull(out, "out");
        this.statePath = Objects.requireNonNull(statePath, "statePath");
        this.rollSupplier = Objects.requireNonNull(rollSupplier, "rollSupplier");
        this.random = Objects.requireNonNull(random, "random");
        this.playerColors = assignColors(this.players);
        this.robberTileId = findInitialRobberTileId();
    }

    public void runGame(int turns) {
        if (turns < 1 || turns > 8192) {
            throw new IllegalArgumentException("turns must be in [1, 8192].");
        }
        writeStateSnapshot();
        for (int turn = 1; turn <= turns; turn++) {
            for (Player current : players) {
                if (!runSinglePlayerTurn(turn, current)) {
                    out.println("Input closed. Game ended.");
                    return;
                }
                if (current.getVictoryPoints() >= 10) {
                    printAction(turn, current.getName(), "Win with " + current.getVictoryPoints() + " VP");
                    writeStateSnapshot();
                    return;
                }
            }
        }
        out.println("Reached max turns without a winner.");
    }

    private boolean runSinglePlayerTurn(int turnId, Player player) {
        boolean rolled = false;
        out.printf("Turn %d - %s (commands: Roll, List, Actions, Build ..., Go)%n", turnId, player.getName());
        printAvailableActions(turnId, player, rolled);
        while (true) {
            out.print("> ");
            if (!scanner.hasNextLine()) {
                return false;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String lower = line.toLowerCase();
            if (lower.equals("roll")) {
                if (rolled) {
                    printAction(turnId, player.getName(), "Invalid: already rolled");
                    continue;
                }
                int roll = rollDice();
                rolled = true;
                printAction(turnId, player.getName(), "Roll " + roll);
                if (roll == 7) {
                    handleRobberRoll(turnId, player);
                } else {
                    distributeResources(roll, turnId);
                }
                writeStateSnapshot();
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (lower.equals("list")) {
                printAction(turnId, player.getName(), "List " + formatResources(player));
                continue;
            }

            if (lower.equals("actions")) {
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (lower.startsWith("build")) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                handleBuildCommand(turnId, player, line);
                writeStateSnapshot();
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (lower.equals("go")) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                printAction(turnId, player.getName(), "Go");
                return true;
            }

            printAction(turnId, player.getName(), "Invalid command");
            out.println("Valid commands: Roll | List | Actions | Build settlement <nodeId> | Build city <nodeId> | Build road <fromNodeId, toNodeId> | Go");
        }
    }

    private int rollDice() {
        return rollSupplier.getAsInt();
    }

    private void printAvailableActions(int turnId, Player player, boolean rolled) {
        List<String> actions = new ArrayList<>();
        if (!rolled) {
            actions.add("Roll");
            actions.add("List");
            actions.add("Actions");
            printAction(turnId, player.getName(), "Available actions: " + String.join(" | ", actions));
            return;
        }

        actions.add("List");
        actions.add("Actions");
        actions.add("Go");

        for (ActionDecision decision : BuildPlanner.availableActions(board, player)) {
            switch (decision.getAction()) {
                case SETTLEMENT -> actions.add("Build settlement " + decision.getTargetId());
                case CITY -> actions.add("Build city " + decision.getTargetId());
                case ROAD -> {
                    Path path = board.getPath(decision.getTargetId());
                    actions.add("Build road " + path.getNodeAId() + ", " + path.getNodeBId());
                }
                case NONE -> {
                }
            }
        }
        printAction(turnId, player.getName(), "Available actions: " + String.join(" | ", actions));
    }

    private void handleBuildCommand(int turnId, Player player, String line) {
        String payload = line.substring(5).trim();
        if (payload.isEmpty()) {
            printAction(turnId, player.getName(), "Invalid build syntax");
            return;
        }

        String[] firstSplit = payload.split("\\s+", 2);
        String kind = firstSplit[0].toLowerCase();
        String args = firstSplit.length > 1 ? firstSplit[1].trim() : "";

        try {
            switch (kind) {
                case "settlement" -> buildSettlementByNode(turnId, player, args);
                case "city" -> buildCityByNode(turnId, player, args);
                case "road" -> buildRoadByNodes(turnId, player, args);
                default -> printAction(turnId, player.getName(), "Invalid build type");
            }
        } catch (NumberFormatException e) {
            printAction(turnId, player.getName(), "Invalid number format");
        }
    }

    private void buildSettlementByNode(int turnId, Player player, String arg) {
        int nodeId = Integer.parseInt(arg);
        Node node = board.getNode(nodeId);
        if (!node.canBuildSettlement(board, player)) {
            printAction(turnId, player.getName(), "Build settlement " + nodeId + " failed: illegal placement");
            return;
        }
        if (!player.canAfford(BuildPlanner.settlementCost())) {
            printAction(turnId, player.getName(), "Build settlement " + nodeId + " failed: insufficient resources");
            return;
        }
        node.claim(player);
        player.spend(BuildPlanner.settlementCost());
        player.addSettlement(nodeId);
        printAction(turnId, player.getName(), "Build settlement " + nodeId);
    }

    private void buildCityByNode(int turnId, Player player, String arg) {
        int nodeId = Integer.parseInt(arg);
        Node node = board.getNode(nodeId);
        if (!node.canUpgradeToCity(player)) {
            printAction(turnId, player.getName(), "Build city " + nodeId + " failed: illegal upgrade");
            return;
        }
        if (!player.canAfford(BuildPlanner.cityCost())) {
            printAction(turnId, player.getName(), "Build city " + nodeId + " failed: insufficient resources");
            return;
        }
        node.upgradeToCity(player);
        player.spend(BuildPlanner.cityCost());
        player.addCity(nodeId);
        printAction(turnId, player.getName(), "Build city " + nodeId);
    }

    private void buildRoadByNodes(int turnId, Player player, String arg) {
        String[] nodeTokens = arg.split(",");
        if (nodeTokens.length != 2) {
            printAction(turnId, player.getName(), "Build road failed: use fromNodeId, toNodeId");
            return;
        }
        int fromNodeId = Integer.parseInt(nodeTokens[0].trim());
        int toNodeId = Integer.parseInt(nodeTokens[1].trim());
        Optional<Path> pathOpt = findPathByNodes(fromNodeId, toNodeId);
        if (pathOpt.isEmpty()) {
            printAction(turnId, player.getName(), "Build road " + fromNodeId + "," + toNodeId + " failed: no path");
            return;
        }
        Path path = pathOpt.get();
        if (!path.canBuildRoad(board, player) || !isRoadConnectedToPlayerNetwork(player, path)) {
            printAction(turnId, player.getName(), "Build road " + fromNodeId + "," + toNodeId + " failed: illegal placement");
            return;
        }
        if (!player.canAfford(BuildPlanner.roadCost())) {
            printAction(turnId, player.getName(), "Build road " + fromNodeId + "," + toNodeId + " failed: insufficient resources");
            return;
        }
        path.claim(player);
        player.spend(BuildPlanner.roadCost());
        player.addRoad(path.getId());
        updateLongestRoad(player, turnId);
        printAction(turnId, player.getName(), "Build road " + fromNodeId + "," + toNodeId);
    }

    private void distributeResources(int roll, int turnId) {
        for (Tile tile : board.tilesForRoll(roll)) {
            if (tile.getId() == robberTileId) {
                continue;
            }
            ResourceType resourceType = tile.getResourceTypeOpt().orElse(null);
            if (resourceType == null) {
                continue;
            }
            for (int nodeId : tile.getAdjacentNodeIds()) {
                Node node = board.getNode(nodeId);
                Optional<Player> ownerOpt = node.getOwner();
                if (ownerOpt.isEmpty()) {
                    continue;
                }
                Player owner = ownerOpt.get();
                int amount = node.getBuilding().productionYield();
                if (amount <= 0) {
                    continue;
                }
                owner.addResource(resourceType, amount);
                printAction(turnId, owner.getName(), "Collect " + resourceType + " x" + amount + " (roll=" + roll + ")");
            }
        }
    }

    private void handleRobberRoll(int turnId, Player roller) {
        printAction(turnId, roller.getName(), "Robber activated");
        for (Player player : players) {
            int totalCards = player.getTotalResourceCount();
            if (totalCards <= 7) {
                continue;
            }
            int toDiscard = totalCards / 2;
            discardRandomCards(player, toDiscard, turnId);
        }
        moveRobberToRandomTile(turnId, roller);
        Tile robberTile = board.getTiles().stream()
                .filter(tile -> tile.getId() == robberTileId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Robber moved to unknown tile " + robberTileId));
        stealRandomCardFromRobbedPlayer(turnId, roller, robberTile);
    }

    private void discardRandomCards(Player player, int count, int turnId) {
        for (int i = 0; i < count; i++) {
            ResourceType discarded = drawRandomResourceCard(player);
            if (discarded == null) {
                break;
            }
            player.spend(Map.of(discarded, 1));
        }
        printAction(turnId, player.getName(), "Discard " + count + " cards");
    }

    private void moveRobberToRandomTile(int turnId, Player roller) {
        List<Tile> candidates = new ArrayList<>(board.getTiles());
        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() > 1) {
            candidates.removeIf(tile -> tile.getId() == robberTileId);
        }
        Tile target = candidates.get(random.nextInt(candidates.size()));
        robberTileId = target.getId();
        printAction(turnId, roller.getName(), "Robber moved to tile " + robberTileId);
    }

    private void stealRandomCardFromRobbedPlayer(int turnId, Player roller, Tile robberTile) {
        List<Player> victims = qualifyingRobberVictims(roller, robberTile);
        if (victims.isEmpty()) {
            printAction(turnId, roller.getName(), "No eligible player to rob");
            return;
        }
        Player victim = victims.get(random.nextInt(victims.size()));
        ResourceType stolen = drawRandomResourceCard(victim);
        if (stolen == null) {
            printAction(turnId, roller.getName(), "No eligible player to rob");
            return;
        }
        victim.spend(Map.of(stolen, 1));
        roller.addResource(stolen, 1);
        printAction(turnId, roller.getName(), "Robbed " + stolen + " from " + victim.getName());
    }

    private List<Player> qualifyingRobberVictims(Player roller, Tile robberTile) {
        Set<Player> adjacentOwners = new HashSet<>();
        for (int nodeId : robberTile.getAdjacentNodeIds()) {
            board.getNode(nodeId).getOwner().ifPresent(adjacentOwners::add);
        }
        List<Player> victims = new ArrayList<>();
        for (Player player : adjacentOwners) {
            if (player.equals(roller)) {
                continue;
            }
            if (player.getTotalResourceCount() <= 0) {
                continue;
            }
            victims.add(player);
        }
        return victims;
    }

    private ResourceType drawRandomResourceCard(Player player) {
        int total = player.getTotalResourceCount();
        if (total <= 0) {
            return null;
        }
        int picked = random.nextInt(total);
        int cumulative = 0;
        for (ResourceType type : ResourceType.values()) {
            cumulative += player.getResourceCount(type);
            if (picked < cumulative) {
                return type;
            }
        }
        return null;
    }

    private int findInitialRobberTileId() {
        for (Tile tile : board.getTiles()) {
            if (tile.getResourceType() == null) {
                return tile.getId();
            }
        }
        return board.getTiles().get(0).getId();
    }

    private void updateLongestRoad(Player player, int turnId) {
        int newLen = player.calculateLongestRoad(board);
        if (newLen <= currentMaxRoadLength) {
            return;
        }
        if (longestRoadHolder != null && !longestRoadHolder.equals(player)) {
            longestRoadHolder.setHasLongestRoad(false);
            printAction(turnId, player.getName(), "Take Longest Road from " + longestRoadHolder.getName());
        }
        player.setHasLongestRoad(true);
        longestRoadHolder = player;
        currentMaxRoadLength = newLen;
        printAction(turnId, player.getName(), "Now holds Longest Road (length=" + newLen + ")");
    }

    private boolean isRoadConnectedToPlayerNetwork(Player player, Path path) {
        return canConnectAtNode(player, path, path.getNodeAId())
                || canConnectAtNode(player, path, path.getNodeBId());
    }

    private boolean canConnectAtNode(Player player, Path candidate, int nodeId) {
        Node node = board.getNode(nodeId);
        if (node.isOwnedBy(player)) {
            return true;
        }
        if (node.isClaimed() && !node.isOwnedBy(player)) {
            return false;
        }
        for (Path adjacent : board.pathsAdjacentToNode(nodeId)) {
            if (adjacent != candidate && adjacent.isOwnedBy(player)) {
                return true;
            }
        }
        return false;
    }

    private Optional<Path> findPathByNodes(int a, int b) {
        for (Path path : board.getPaths()) {
            boolean direct = path.getNodeAId() == a && path.getNodeBId() == b;
            boolean reverse = path.getNodeAId() == b && path.getNodeBId() == a;
            if (direct || reverse) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    private void writeStateSnapshot() {
        List<Path> roads = new ArrayList<>(board.getPaths());
        roads.sort(Comparator.comparingInt(Path::getId));
        List<Node> nodes = new ArrayList<>(board.getNodes());
        nodes.sort(Comparator.comparingInt(Node::getId));

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"robberTileId\": ").append(robberTileId).append(",\n");
        json.append("  \"roads\": [\n");
        boolean firstRoad = true;
        for (Path road : roads) {
            Optional<Player> ownerOpt = road.getOwner();
            if (ownerOpt.isEmpty()) {
                continue;
            }
            if (!firstRoad) {
                json.append(",\n");
            }
            Player owner = ownerOpt.get();
            json.append("    { \"a\": ").append(road.getNodeAId())
                    .append(", \"b\": ").append(road.getNodeBId())
                    .append(", \"owner\": \"").append(colorOf(owner)).append("\" }");
            firstRoad = false;
        }
        json.append("\n  ],\n");
        json.append("  \"buildings\": [\n");
        boolean firstBuilding = true;
        for (Node node : nodes) {
            Optional<Player> ownerOpt = node.getOwner();
            if (ownerOpt.isEmpty()) {
                continue;
            }
            if (!firstBuilding) {
                json.append(",\n");
            }
            Player owner = ownerOpt.get();
            String type = node.getBuilding().getType() == BuildingType.CITY ? "CITY" : "SETTLEMENT";
            json.append("    { \"node\": ").append(node.getId())
                    .append(", \"owner\": \"").append(colorOf(owner))
                    .append("\", \"type\": \"").append(type).append("\" }");
            firstBuilding = false;
        }
        json.append("\n  ]\n");
        json.append("}\n");

        try {
            if (statePath.getParent() != null) {
                Files.createDirectories(statePath.getParent());
            }
            Files.writeString(statePath, json.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write state file: " + statePath, e);
        }
    }

    private static Map<Player, String> assignColors(List<Player> players) {
        Map<Player, String> colors = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            colors.put(players.get(i), COLOR_ORDER.get(i % COLOR_ORDER.size()));
        }
        return colors;
    }

    private String colorOf(Player player) {
        return playerColors.getOrDefault(player, "WHITE");
    }

    private static String formatResources(Player player) {
        StringBuilder builder = new StringBuilder();
        for (ResourceType type : ResourceType.values()) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(type).append("=").append(player.getResourceCount(type));
        }
        return builder.toString();
    }

    private void printAction(int turnId, String playerId, String action) {
        out.printf("[%d] / [%s]: %s%n", turnId, playerId, action);
    }
}
