package top.cskit.json.testkit;

import top.cskit.json.JsonField;

/**
 * Shared test entity: an employee, demonstrating {@code @JsonField}.
 *
 * <p>Used by the annotation translation layer tests to verify all three
 * backends (gson / fastjson / jackson) behave identically on the same
 * annotation set:
 * <ul>
 *   <li>{@code ename}: renamed JSON field</li>
 *   <li>{@code cost}: deserialize only (serialize=false)</li>
 *   <li>{@code profit}: serialize only (deserialize=false)</li>
 * </ul>
 */
public class Employee {

    @JsonField("ename")
    private String name;

    private double salary;

    @JsonField(serialize = false)
    private double cost;

    @JsonField(deserialize = false)
    private double profit;

    public Employee() {
    }

    public Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }
}
