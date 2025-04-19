package src;

import java.util.*;
import java.util.stream.Collectors;

public class CloudEnvironment {
    private final List<Node> nodes;
    private final double EVAPORATION = 0.1;
    private final double INCENTIVE   = 0.2;
    private final double PUNISHMENT  = 0.15;

    // Metrics
    private int antCount, transferCount, failureCount;

    public CloudEnvironment(List<Node> nodes) {
        this.nodes = nodes;
        nodes.forEach(n -> n.addNeighbors(nodes));
    }

    public void resetMetrics() {
        antCount = transferCount = failureCount = 0;
    }

    /** Step 7: generate forward ant for each overloaded VM */
    public void reactiveBalance() {
        for (Node vm : nodes) {
            if (vm.isOverloaded()) {
                antCount++;
                ForwardAnt ant = new ForwardAnt(vm);
                boolean ok = ant.searchForCandidate();
                if (ok) transferCount++;
                else    failureCount++;
            }
        }
    }

    public int getAntCount()      { return antCount; }
    public int getTransferCount() { return transferCount; }
    public int getFailureCount()  { return failureCount; }

    private class ForwardAnt {
        private Node current;
        private final List<Node> path = new ArrayList<>();
        private long timer;  // step 13: simulate backward ant timer

        ForwardAnt(Node start) {
            this.current = start;
            path.add(start);
        }

        /**
         * Performs up to 3 hops (step 9–11).
         * On finding underloaded, does backward ant (12–16),
         * updates pheromones (17–20), and transfers load.
         */
        boolean searchForCandidate() {
            for (int hop = 0; hop < 3; hop++) {
                Optional<Node> next = selectNextNode();
                if (!next.isPresent()) break;

                // Step 9–10: move
                current = next.get();
                path.add(current);

                // Step 14: pheromone by forward ant
                current.updatePheromone(-PUNISHMENT);

                // Step 15: start timer for backward ant
                timer = System.currentTimeMillis();

                if (current.isUnderloaded()) {
                    // Step 12: backward ant
                    triggerBackwardAnt();
                    // Step 17: successful task → reward
                    current.updatePheromone(INCENTIVE);
                    performLoadTransfer(path.get(0), current);
                    return true;
                }
            }
            // Step 19–20: task failed → punish origin
            path.get(0).updatePheromone(-PUNISHMENT);
            return false;
        }

        private Optional<Node> selectNextNode() {
            List<Node> cand = current.getNeighbors().stream()
                .filter(n -> !path.contains(n))
                .collect(Collectors.toList());
            if (cand.isEmpty()) return Optional.empty();

            double total = cand.stream().mapToDouble(Node::getPheromone).sum();
            double pick  = Math.random() * total, cum = 0;
            for (Node n : cand) {
                cum += n.getPheromone();
                if (cum >= pick) return Optional.of(n);
            }
            return Optional.of(cand.get(cand.size()-1));
        }

        /** Step 12–16: backward ant releases pheromone if timer > 0 */
        private void triggerBackwardAnt() {
            long elapsed = System.currentTimeMillis() - timer;
            if (elapsed >= 0) {  // always true, but matches step 15–16
                Collections.reverse(path);
                for (Node n : path) {
                    n.updatePheromone(INCENTIVE * 2);
                }
            }
        }

        /** Step 23: transfer 30% load */
        private void performLoadTransfer(Node src, Node dst) {
            double transfer = src.getCurrentLoad() * 0.3;
            Task fake = new Task(-1, transfer * src.getCurrentMips(), Double.MAX_VALUE);
            if (dst.canHandle(fake)) {
                src.removeNormalizedLoad(transfer);
                dst.addTask(fake);
            }
        }
    }
}
