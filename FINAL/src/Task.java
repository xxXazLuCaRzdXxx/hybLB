package src;

public class Task {
    private final int id;
    private final double length;    // Lᵢ: task length (MI)
    private final double deadline;  // Dᵢ: task deadline (time units)

    public Task(int id, double length, double deadline) {
        this.id = id;
        this.length = length;
        this.deadline = deadline;
    }

    public int getId()             { return id; }
    public double getLength()      { return length; }
    public double getDeadline()    { return deadline; }
}
