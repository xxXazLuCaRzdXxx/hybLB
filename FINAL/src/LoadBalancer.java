package src;

import java.util.*;
import java.util.stream.Collectors;

public class LoadBalancer {
    private final List<Node> nodes;
    private final Map<Node, Double> vmPrices;

    /**
     * @param nodes    list of VMs
     * @param vmPrices map for step 28: price per VM
     */
    public LoadBalancer(List<Node> nodes, Map<Node, Double> vmPrices) {
        this.nodes    = nodes;
        this.vmPrices = vmPrices;
    }

    /**
     * Steps 1–12: Phase 1 (OCT) + Steps 13–27: Phase 2 (EFT)
     */
    public void scheduleTasks(List<Task> tasks) {
        // Step 2: compute OCTᵢ = (ΣLᵢ)/(ΣMIPSⱼ) × MIPSᵢ
        double totalLen  = tasks.stream().mapToDouble(Task::getLength).sum();
        double totalMips = nodes.stream().mapToDouble(Node::getCurrentMips).sum();
        Map<Node, Double> OCT = new HashMap<>();
        for (Node vm : nodes) {
            OCT.put(vm, totalLen/totalMips * vm.getCurrentMips());
        }

        List<Task> GQ = new ArrayList<>(tasks);

        // Phase 1: Balancing by OCT  (lines 4–12)
        for (Node vm : nodes) {
            double vmOCT = OCT.get(vm);
            List<Task> toAssign = GQ.stream()
                .filter(t -> vm.estimatedCompletion(t) <= vmOCT)
                .collect(Collectors.toList());
            for (Task t : toAssign) {
                vm.addTask(t);
                GQ.remove(t);
            }
        }

        // Phase 2: Allocation by EFT  (lines 13–27)
        for (Task t : new ArrayList<>(GQ)) {
            double minEFT = Double.MAX_VALUE;
            Node  best   = null;
            for (Node vm : nodes) {
                double eft = vm.estimatedCompletion(t);
                if (eft < minEFT) {
                    minEFT = eft;
                    best   = vm;
                }
            }
            best.addTask(t);
            GQ.remove(t);
        }

        // Step 28–29: Associate price & map queues back to VMs
        for (Node vm : nodes) {
            vm.setPrice(vmPrices.getOrDefault(vm, 0.0));
        }
    }
}
