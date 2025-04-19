// src/Main.java
package src;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Scenario 1: Default end‑to‑end (HLBZID → ACO success → no SLA violation)
        runScenario(
            "Default Scenario",
            generateFreshNodes(),
            generateDefaultTasks()
        );

        // Scenario 2: SLA Reconfiguration (no ACO, just SLA CPU bump)
        runScenario(
            "SLA Reconfiguration Scenario",
            generateFreshNodes(),
            generateSLAReconfigTasks()
        );

        // Scenario 3: ACO Failure (no tasks, forced overload → no candidate found)
        runScenario(
            "ACO Failure Scenario",
            generateForcedOverloadNodes(),
            Collections.emptyList()
        );
    }

    private static void runScenario(String title, List<Node> nodes, List<Task> tasks) {
        System.out.println("\n\n=== " + title + " ===");

        // 1) HLBZID Scheduling
        if (!tasks.isEmpty()) {
            LoadBalancer lb = new LoadBalancer(nodes, generatePrices(nodes));
            lb.scheduleTasks(tasks);
            System.out.println("\n-- After HLBZID --");
            printNodes(nodes);
        }

        // 2) ACO Reactive Balance
        CloudEnvironment aco = new CloudEnvironment(nodes);
        aco.resetMetrics();
        aco.reactiveBalance();
        System.out.println("\n-- After ACO Reactive Balance --");
        printNodes(nodes);
        System.out.printf(
            "ACO: ants=%d, transfers=%d, failures=%d%n",
            aco.getAntCount(), aco.getTransferCount(), aco.getFailureCount()
        );

        // 3) SLA Enforcement
        SLAEnforcer.resetMetrics();
        SLAEnforcer.enforceSLA(nodes);
        System.out.println("\n-- After SLA Enforcement --");
        printNodes(nodes);
        System.out.printf("SLA migrations=%d%n", SLAEnforcer.getMigrationCount());

        // 4) Performance Metrics
        printMetrics(nodes, aco.getAntCount(), aco.getTransferCount(),
                     aco.getFailureCount(), SLAEnforcer.getMigrationCount());
        
        // 5) Reset nodes for next scenario
        clearAll(nodes);
    }

    // ── Scenario generators ────────────────────────────────────────────────────

    private static List<Node> generateFreshNodes() {
        double[] init = {150,150,100,100,100};
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < init.length; i++) {
            // host max = 2× initial
            nodes.add(new Node(i, init[i], init[i]*2));
        }
        return nodes;
    }

    private static List<Task> generateDefaultTasks() {
        // Mixed lengths & deadlines so HLBZID & ACO both trigger, no SLA
        return List.of(
            new Task(1, 40, 5),
            new Task(2, 80, 10),
            new Task(3, 60, 8),
            new Task(4, 30, 4),
            new Task(5, 100, 20),
            new Task(6, 50, 12)
        );
    }

    private static List<Task> generateSLAReconfigTasks() {
        // Single task: normalized load <0.9 → no ACO; Cij>DDL → triggers CPU reconfig
        return List.of(
            new Task(99, 120, 0.5)  // on best VM: 120/150=0.8 load, but 0.8>0.5 deadline
        );
    }

    private static List<Node> generateForcedOverloadNodes() {
        List<Node> nodes = generateFreshNodes();
        // Fill each to 50% so none underloaded
        for (Node n : nodes) {
            double halfNorm = 0.5;
            double length  = halfNorm * n.getCurrentMips();
            n.addTask(new Task(-1, length, Double.MAX_VALUE));
        }
        // Overload node 0
        Node root = nodes.get(0);
        double extra = root.getCurrentMips() * 1.0;
        root.addTask(new Task(-2, extra, Double.MAX_VALUE));
        return nodes;
    }

    private static Map<Node,Double> generatePrices(List<Node> nodes) {
        Map<Node,Double> mp = new HashMap<>();
        for (Node n : nodes) {
            // e.g. $0.01 per MIPS
            mp.put(n, n.getCurrentMips() * 0.01);
        }
        return mp;
    }

    // ── Printing & Metrics ────────────────────────────────────────────────────

    private static void printNodes(List<Node> nodes) {
        nodes.forEach(n -> System.out.println("  " + n));
    }

    private static void printMetrics(
        List<Node> nodes,
        int antCount, int transfers, int failures,
        int migrations
    ) {
        // Makespan = max normalized load
        double makespan = nodes.stream()
            .mapToDouble(Node::getCurrentLoad)
            .max().orElse(0.0);
        // Avg utilization
        double avgUtil = nodes.stream()
            .mapToDouble(Node::getCurrentLoad)
            .average().orElse(0.0);
        // Std‑dev imbalance
        double variance = nodes.stream()
            .mapToDouble(n -> Math.pow(n.getCurrentLoad() - avgUtil, 2))
            .average().orElse(0.0);
        double imbalance = Math.sqrt(variance);
        // Total cost = Σ(price_per_MIPS * used_MIPS)
        double cost = nodes.stream()
            .mapToDouble(n -> n.getPrice() * (n.getCurrentLoad() * n.getCurrentMips()))
            .sum();

        System.out.println("\n-- Performance Metrics --");
        System.out.printf("Makespan (norm time): %.3f%n", makespan);
        System.out.printf("Avg util (load):      %.3f%n", avgUtil);
        System.out.printf("Imbalance (std dev):  %.3f%n", imbalance);
        System.out.printf("Total cost ($):       %.2f%n", cost);
        System.out.printf("ACO ants:             %d%n", antCount);
        System.out.printf("ACO transfers:        %d%n", transfers);
        System.out.printf("ACO failures:         %d%n", failures);
        System.out.printf("SLA migrations:       %d%n", migrations);
    }

    private static void clearAll(List<Node> nodes) {
        for (Node n : nodes) {
            n.getTasks().clear();
            n.removeNormalizedLoad(n.getCurrentLoad());
            // reset pheromone & CPU & price
            // (if you want to re‑use same Node objects, you may need setters here)
        }
    }
}
