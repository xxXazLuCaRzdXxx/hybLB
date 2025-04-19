package src;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private final int id;
    private double currentMips;       // current CPU allocation (MIPS)
    private final double maxMips;     // host capacity limit (MIPS)
    private double currentLoad = 0.0; // normalized load = Σ(length)/currentMips
    private double pheromone = 1.0;   // for ACO
    private double price = 0.0;       // step 28: VM price
    private final List<Node> neighbors = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();

    public Node(int id, double initialMips, double maxMips) {
        this.id = id;
        this.currentMips = initialMips;
        this.maxMips     = maxMips;
    }

    // -------------- Core getters --------------
    public int getId()                   { return id; }
    public double getCurrentMips()       { return currentMips; }
    public double getMaxMips()           { return maxMips; }
    public double getCurrentLoad()       { return currentLoad; }
    public double getPheromone()         { return pheromone; }
    public double getPrice()             { return price; }
    public List<Node> getNeighbors()     { return neighbors; }
    public List<Task> getTasks()         { return tasks; }

    public void addNeighbors(List<Node> ns) { neighbors.addAll(ns); }
    public void setPrice(double p)           { price = p; }

    // -------------- Scheduling --------------
    /** normalized load if this task added */
    public double estimatedCompletion(Task t) {
        return currentLoad + t.getLength()/currentMips;
    }

    public boolean canHandle(Task t) {
        return estimatedCompletion(t) <= 1.0;
    }

    public void addTask(Task t) {
        tasks.add(t);
        currentLoad += t.getLength()/currentMips;
    }

    public void removeNormalizedLoad(double amt) {
        currentLoad = Math.max(0.0, currentLoad - amt);
    }

    // -------------- ACO --------------
    public void evaporatePheromone(double rate) {
        pheromone *= (1.0 - rate);
    }

    public void updatePheromone(double delta) {
        pheromone = Math.max(0.1, pheromone + delta);
    }

    public boolean isOverloaded()  { return currentLoad > 0.9; }
    public boolean isUnderloaded() { return currentLoad < 0.2; }

    // -------------- SLA reconfiguration --------------
    /**
     * Step 15–17 of Algorithm 3: try to increase CPU to host max
     * @return true if reconfiguration succeeded, false if at max
     */
    public boolean reconfigureCPU() {
        if (currentMips < maxMips) {
            currentMips = maxMips;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
          "Node[%d | load=%.2f/1.00 | MIPS=%.1f/%.1f | pher=%.2f | price=%.2f | status=%s]",
          id, currentLoad, currentMips, maxMips, pheromone, price, getStatus()
        );
    }

    private String getStatus() {
        if (isOverloaded())  return "OVERLOADED";
        if (isUnderloaded()) return "UNDERLOADED";
        return "STABLE";
    }
}
