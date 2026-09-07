package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonField;
import top.cskit.json.gson.GsonJsonAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 框架统一注解 @JsonField 支持测试（Gson 翻译层）
 * <p>
 * 验证能力：
 * 1. 字段重命名：{@code @JsonField("ename")} → JSON 输出 ename
 * 2. 序列化开关：{@code @JsonField(serialize=false)} → 不输出
 * 3. 反序列化开关：{@code @JsonField(deserialize=false)} → 不接收
 * 4. 双向往返：rename 字段可逆
 */
class JsonFieldAnnotationTest {

    static final JsonAdapter GSON = new GsonJsonAdapter();

    /** 员工：演示字段重命名 + 只读 + 只写（老板-会计场景） */
    public static class Employee {
        @JsonField("ename")
        private String name;

        private double salary;

        /** 会计填的，从 JSON 读进来但不序列化出去（类比 @Expose(serialize=false)） */
        @JsonField(serialize = false)
        private double cost;

        /** 老板自己算的，序列化给外面看但不从 JSON 读 */
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

    @Test
    @DisplayName("字段重命名：@JsonField(\"ename\") 序列化/反序列化都用 ename")
    void fieldRename() {
        String json = GSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("序列化: " + json);

        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON 应使用注解名 ename");
        assertFalse(json.contains("\"name\""), "不应输出 Java 字段名 name");

        // 反序列化：ename → name
        Employee back = GSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        System.out.println("反序列化: name=" + back.getName() + ", salary=" + back.getSalary());
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("序列化开关：@JsonField(serialize=false) 不输出，但可接收")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0); // 会计填的成本，不应发出去

        String json = GSON.toJson(e);
        System.out.println("序列化: " + json);
        assertFalse(json.contains("cost"), "serialize=false 字段不应输出");
        assertTrue(json.contains("\"ename\""), "普通字段正常输出");

        // 会计发回时 cost 在 JSON 里，应该被接收
        Employee back = GSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        System.out.println("接收 cost=" + back.getCost());
        assertEquals(8000.0, back.getCost(), 0.001);
    }

    @Test
    @DisplayName("反序列化开关：@JsonField(deserialize=false) 可输出但拒绝接收")
    void deserializeOff() {
        // 老板序列化时 profit 输出（虽然可能为 0，由 getter 计算）
        String json = GSON.toJson(new Employee("Boss", 100000.0));
        System.out.println("序列化: " + json);

        // 会计传回 profit 但老板不让外部写入（自己算）
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = GSON.fromJson(fromAccountant, Employee.class);
        System.out.println("反序列化后 profit（应忽略外部值）: " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false 应拒绝外部写入");
    }

    @Test
    @DisplayName("往返：rename 字段序列化后反序列化不丢数据")
    void roundTrip() {
        Employee e = new Employee("Jerry", 9000.5);
        String json = GSON.toJson(e);
        Employee back = GSON.fromJson(json, Employee.class);
        assertEquals("Jerry", back.getName());
        assertEquals(9000.5, back.getSalary(), 0.001);
    }
}
