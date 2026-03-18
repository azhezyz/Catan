package Catan;

public final class BuildRoadCommand implements GameCommand {
    private final Board board;
    private final Player player;
    private final int pathId;
    private String failureReason = "";
    private boolean executed = false;

    public BuildRoadCommand(Board board, Player player, int pathId) {
        this.board = board;
        this.player = player;
        this.pathId = pathId;
    }

    @Override
    public boolean execute() {
        Path path = board.getPath(pathId);
        if (!path.canBuildRoad(board, player) || !isRoadConnectedToPlayerNetwork(path)) {
            failureReason = "illegal placement";
            return false;
        }
        if (!player.canAfford(BuildPlanner.roadCost())) {
            failureReason = "insufficient resources";
            return false;
        }
        path.claim(player);
        player.spend(BuildPlanner.roadCost());
        player.addRoad(pathId);
        executed = true;
        failureReason = "";
        return true;
    }

    @Override
    public void undo() {
        if (!executed) {
            return;
        }
        Path path = board.getPath(pathId);
        path.unclaim(player);
        player.removeRoad(pathId);
        player.refund(BuildPlanner.roadCost());
        executed = false;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    private boolean isRoadConnectedToPlayerNetwork(Path path) {
        return canConnectAtNode(path, path.getNodeAId()) || canConnectAtNode(path, path.getNodeBId());
    }

    private boolean canConnectAtNode(Path candidate, int nodeId) {
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
}
