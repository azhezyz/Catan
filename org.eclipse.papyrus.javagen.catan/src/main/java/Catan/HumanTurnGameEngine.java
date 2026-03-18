package Catan;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HumanTurnGameEngine {
    private static final List<String> COLOR_ORDER = List.of("RED", "BLUE", "ORANGE", "WHITE");
    private static final Pattern ROLL_COMMAND = Pattern.compile("(?i)^roll$");
    private static final Pattern LIST_COMMAND = Pattern.compile("(?i)^list$");
    private static final Pattern ACTIONS_COMMAND = Pattern.compile("(?i)^actions$");
    private static final Pattern GO_COMMAND = Pattern.compile("(?i)^go$");
    private static final Pattern UNDO_COMMAND = Pattern.compile("(?i)^undo$");
    private static final Pattern REDO_COMMAND = Pattern.compile("(?i)^redo$");
    private static final Pattern BUILD_COMMAND = Pattern.compile("(?i)^build\\s*(.*)$");
    private static final Pattern BUILD_KIND_COMMAND = Pattern.compile("(?i)^(\\S+)(?:\\s+(.*))?$");

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
        CommandManager commandManager = new CommandManager();
        out.printf("Turn %d - %s (commands: Roll, List, Actions, Build ..., Undo, Redo, Go)%n", turnId, player.getName());
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

            if (ROLL_COMMAND.matcher(line).matches()) {
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

            if (LIST_COMMAND.matcher(line).matches()) {
                printAction(turnId, player.getName(), "List " + formatResources(player));
                continue;
            }

            if (ACTIONS_COMMAND.matcher(line).matches()) {
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (UNDO_COMMAND.matcher(line).matches()) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                if (!commandManager.undoLast()) {
                    printAction(turnId, player.getName(), "Undo failed: " + commandManager.getLastFailureReason());
                } else {
                    recomputeLongestRoadState();
                    printAction(turnId, player.getName(), "Undo");
                    writeStateSnapshot();
                }
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (REDO_COMMAND.matcher(line).matches()) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                if (!commandManager.redoLast()) {
                    printAction(turnId, player.getName(), "Redo failed: " + commandManager.getLastFailureReason());
                } else {
                    recomputeLongestRoadState();
                    printAction(turnId, player.getName(), "Redo");
                    writeStateSnapshot();
                }
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            Matcher buildMatcher = BUILD_COMMAND.matcher(line);
            if (buildMatcher.matches()) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                handleBuildCommand(turnId, player, buildMatcher.group(1), commandManager);
                writeStateSnapshot();
                printAvailableActions(turnId, player, rolled);
                continue;
            }

            if (GO_COMMAND.matcher(line).matches()) {
                if (!rolled) {
                    printAction(turnId, player.getName(), "Invalid: roll first");
                    continue;
                }
                printAction(turnId, player.getName(), "Go");
                return true;
            }

            printAction(turnId, player.getName(), "Invalid command");
            out.println("Valid commands: Roll | List | Actions | Build settlement <nodeId> | Build city <nodeId> | Build road <fromNodeId, toNodeId> | Undo | Redo | Go");
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
        actions.add("Undo");
        actions.add("Redo");
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

    private void handleBuildCommand(int turnId, Player player, String payload, CommandManager commandManager) {
        payload = payload == null ? "" : payload.trim();
        if (payload.isEmpty()) {
            printAction(turnId, player.getName(), "Invalid build syntax");
            return;
        }

        Matcher buildKindMatcher = BUILD_KIND_COMMAND.matcher(payload);
        if (!buildKindMatcher.matches()) {
            printAction(turnId, player.getName(), "Invalid build syntax");
            return;
        }

        String kind = buildKindMatcher.group(1).toLowerCase(Locale.ROOT);
        String args = buildKindMatcher.group(2) == null ? "" : buildKindMatcher.group(2).trim();

        try {
            switch (kind) {
                case "settlement" -> buildSettlementByNode(turnId, player, args, commandManager);
                case "city" -> buildCityByNode(turnId, player, args, commandManager);
                case "road" -> buildRoadByNodes(turnId, player, args, commandManager);
                default -> printAction(turnId, player.getName(), "Invalid build type");
            }
        } catch (NumberFormatException e) {
            printAction(turnId, player.getName(), "Invalid number format");
        }
    }

    private void buildSettlementByNode(int turnId, Player player, String arg, CommandManager commandManager) {
        int nodeId = Integer.parseInt(arg);
        BuildSettlementCommand command = new BuildSettlementCommand(board, player, nodeId);
        if (!commandManager.executeCommand(command)) {
            printAction(turnId, player.getName(), "Build settlement " + nodeId + " failed: " + command.getFailureReason());
            return;
        }
        recomputeLongestRoadState();
        printAction(turnId, player.getName(), "Build settlement " + nodeId);
    }

    private void buildCityByNode(int turnId, Player player, String arg, CommandManager commandManager) {
        int nodeId = Integer.parseInt(arg);
        BuildCityCommand command = new BuildCityCommand(board, player, nodeId);
        if (!commandManager.executeCommand(command)) {
            printAction(turnId, player.getName(), "Build city " + nodeId + " failed: " + command.getFailureReason());
            return;
        }
        recomputeLongestRoadState();
        printAction(turnId, player.getName(), "Build city " + nodeId);
    }

    private void buildRoadByNodes(int turnId, Player player, String arg, CommandManager commandManager) {
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
        BuildRoadCommand command = new BuildRoadCommand(board, player, path.getId());
        if (!commandManager.executeCommand(command)) {
            printAction(turnId, player.getName(), "Build road " + fromNodeId + "," + toNodeId + " failed: " + command.getFailureReason());
            return;
        }
        recomputeLongestRoadState();
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
        int rollerCards = roller.getTotalResourceCount();
        if (rollerCards > 7) {
            int toDiscard = rollerCards / 2;
            discardRandomCards(roller, toDiscard, turnId);
        }
        moveRobberAndStealFromRandomPlayer(turnId, roller);
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

    private void moveRobberAndStealFromRandomPlayer(int turnId, Player roller) {
        List<Player> victims = new ArrayList<>();
        for (Player player : players) {
            if (player.equals(roller)) {
                continue;
            }
            if (player.getTotalResourceCount() <= 0) {
                continue;
            }
            if (tileIdsAdjacentToPlayer(player).isEmpty()) {
                continue;
            }
            victims.add(player);
        }
        if (victims.isEmpty()) {
            printAction(turnId, roller.getName(), "No eligible player to rob");
            return;
        }

        Player victim = victims.get(random.nextInt(victims.size()));
        List<Integer> victimTileIds = tileIdsAdjacentToPlayer(victim);
        List<Integer> moveCandidates = new ArrayList<>(victimTileIds);
        if (moveCandidates.size() > 1) {
            moveCandidates.removeIf(tileId -> tileId == robberTileId);
        }
        int targetTileId = moveCandidates.get(random.nextInt(moveCandidates.size()));
        robberTileId = targetTileId;
        printAction(turnId, roller.getName(), "Robber moved to tile " + robberTileId + " (" + victim.getName() + ")");

        ResourceType stolen = drawRandomResourceCard(victim);
        if (stolen == null) {
            printAction(turnId, roller.getName(), "No eligible player to rob");
            return;
        }
        victim.spend(Map.of(stolen, 1));
        roller.addResource(stolen, 1);
        printAction(turnId, roller.getName(), "Robbed " + stolen + " from " + victim.getName());
    }

    private List<Integer> tileIdsAdjacentToPlayer(Player player) {
        Set<Integer> tileIds = new HashSet<>();
        for (Node node : board.getNodes()) {
            if (!node.isOwnedBy(player)) {
                continue;
            }
            tileIds.addAll(node.getAdjacentTileIds());
        }
        return new ArrayList<>(tileIds);
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

    private void recomputeLongestRoadState() {
        Map<Player, Integer> lengths = new HashMap<>();
        for (Player candidate : players) {
            candidate.setHasLongestRoad(false);
            lengths.put(candidate, candidate.calculateLongestRoad(board));
        }

        int bestLength = 4;
        for (Integer length : lengths.values()) {
            if (length > bestLength) {
                bestLength = length;
            }
        }

        if (bestLength <= 4) {
            longestRoadHolder = null;
            currentMaxRoadLength = 4;
            return;
        }

        Player holderCandidate = null;
        if (longestRoadHolder != null && lengths.getOrDefault(longestRoadHolder, 0) == bestLength) {
            holderCandidate = longestRoadHolder;
        } else {
            for (Player candidate : players) {
                if (lengths.getOrDefault(candidate, 0) == bestLength) {
                    holderCandidate = candidate;
                    break;
                }
            }
        }

        if (holderCandidate != null) {
            holderCandidate.setHasLongestRoad(true);
        }
        longestRoadHolder = holderCandidate;
        currentMaxRoadLength = bestLength;
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
