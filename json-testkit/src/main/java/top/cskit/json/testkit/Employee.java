package top.cskit.json.testkit;

import top.cskit.json.JsonField;

/**
 * 共享测试实体：员工（演示 {@code @JsonField} 注解）。
 * <p>
 * 注解翻译层测试统一使用本实体，验证三库（gson / fastjson / jackson）
 * 对同一套 {@code @JsonField} 注解行为一致：
 * <ul>
 *   <li>{@code ename}：字段重命名（JSON 用 ename）</li>
 *   <li>{@code cost}：只读不写（serialize=false）</li>
 *   <li>{@code profit}：只写不读（deserialize=false）</li>
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
