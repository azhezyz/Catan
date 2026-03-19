package Catan;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class StateSnapshotObserver implements GameObserver {
    private final java.nio.file.Path statePath;

    public StateSnapshotObserver(java.nio.file.Path statePath) {
        this.statePath = Objects.requireNonNull(statePath, "statePath");
    }

    @Override
    public void onEvent(GameEvent event) {
        if (!(event instanceof GameEvent.StateChangedEvent stateChangedEvent)) {
            return;
        }
        writeStateSnapshot(stateChangedEvent.robberTileId(), stateChangedEvent.board(), stateChangedEvent.playerColors());
    }

    private void writeStateSnapshot(int robberTileId, Board board, Map<Player, String> playerColors) {
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
                    .append(", \"owner\": \"").append(colorOf(owner, playerColors)).append("\" }");
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
                    .append(", \"owner\": \"").append(colorOf(owner, playerColors))
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

    private static String colorOf(Player player, Map<Player, String> playerColors) {
        return playerColors.getOrDefault(player, "WHITE");
    }
}
