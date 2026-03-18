package Catan;

public final class BuildCityCommand implements GameCommand {
    private final Board board;
    private final Player player;
    private final int nodeId;
    private String failureReason = "";
    private boolean executed = false;

    public BuildCityCommand(Board board, Player player, int nodeId) {
        this.board = board;
        this.player = player;
        this.nodeId = nodeId;
    }

    @Override
    public boolean execute() {
        Node node = board.getNode(nodeId);
        if (!node.canUpgradeToCity(player)) {
            failureReason = "illegal upgrade";
            return false;
        }
        if (!player.canAfford(BuildPlanner.cityCost())) {
            failureReason = "insufficient resources";
            return false;
        }
        node.upgradeToCity(player);
        player.spend(BuildPlanner.cityCost());
        player.addCity(nodeId);
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
        node.downgradeCityToSettlement(player);
        player.removeCity(nodeId);
        player.refund(BuildPlanner.cityCost());
        executed = false;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }
}
