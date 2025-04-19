package src;

import java.util.Comparator;
import java.util.List;

public class SLAEnforcer {
    private static int migrationCount = 0;

    public static void resetMetrics() {
        migrationCount = 0;
    }

    /**
     * Steps 9–27 of Algorithm 3:
     * For each VM, compute Cᵢⱼ & Vᵢⱼ; on violation:
     * - try reconfigureCPU (step 15–16)
     * - else migrateAll (17–19)
     * repeat until no violations
     */
    public static void enforceSLA(List<Node> nodes) {
        for (Node vm : nodes) {
            boolean repeat;
            do {
                repeat = false;
                double elapsed = 0.0;
                for (Task t : vm.getTasks()) {
                    double Cij = elapsed + t.getLength()/vm.getCurrentMips();
                    double Vij = Math.abs(Cij - t.getDeadline());
                    if (Cij > t.getDeadline()) {
                        // SLA violation
                        if (vm.reconfigureCPU()) {
                            // step 21: recompute Cij after CPU bump
                            repeat = true;
                            break;
                        } else {
                            migrateAll(vm, nodes);
                            migrationCount++;
                        }
                    }
                    elapsed += t.getLength()/vm.getCurrentMips();
                }
            } while (repeat);
        }
    }

    private static void migrateAll(Node from, List<Node> nodes) {
        Node target = nodes.stream()
            .filter(n -> n != from)
            .min(Comparator.comparingDouble(Node::getCurrentLoad))
            .orElse(from);

        for (Task t : from.getTasks()) {
            target.addTask(t);
        }
        from.getTasks().clear();
        from.removeNormalizedLoad(from.getCurrentLoad());
    }

    public static int getMigrationCount() {
        return migrationCount;
    }
}
