package Catan;

public final class BuildSettlementCommand implements GameCommand {
    private final Board board;
    private final Player player;
    private final int nodeId;
    private String failureReason = "";
    private boolean executed = false;

    public BuildSettlementCommand(Board board, Player player, int nodeId) {
        this.board = board;
        this.player = player;
        this.nodeId = nodeId;
    }

    @Override
    public boolean execute() {
        Node node = board.getNode(nodeId);
        if (!node.canBuildSettlement(board, player)) {
            failureReason = "illegal placement";
            return false;
        }
        if (!player.canAfford(BuildPlanner.settlementCost())) {
            failureReason = "insufficient resources";
            return false;
        }
        node.claim(player);
        player.spend(BuildPlanner.settlementCost());
        player.addSettlement(nodeId);
        executed = true;
        failureReason = "";
        return true;
    }

    @Override
    public void undo() {
        if (!executed) {
            return;
        }
        Node node = board.getNode(nodeId);
        node.unclaim(player);
        player.removeSettlement(nodeId);
        player.refund(BuildPlanner.settlementCost());
        executed = false;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }
}
