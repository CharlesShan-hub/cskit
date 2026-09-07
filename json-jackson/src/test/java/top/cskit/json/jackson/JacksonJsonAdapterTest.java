package top.cskit.json.jackson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonField;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jackson 适配器测试：功能 + 注解翻译层验证
 * <p>
 * 三库横向对比：同一个实体、同一段业务代码，gson / fastjson / jackson 结果一致。
 */
class JacksonJsonAdapterTest {

    static final JsonAdapter JACKSON = new JacksonJsonAdapter();

    public static class Animal {
        private String name;
        private int birthYear;

        public Animal() {
        }

        public Animal(String name, int birthYear) {
            this.name = name;
            this.birthYear = birthYear;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(int birthYear) {
            this.birthYear = birthYear;
        }
    }

    /** 注解实体：字段重命名 + 序列化/反序列化开关 */
    public static class Employee {
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

    // region 1. 基础功能

    @Test
    @DisplayName("对象往返：序列化 + 反序列化")
    void roundTrip() {
        String json = JACKSON.toJson(new Animal("Tom", 1938));
        System.out.println("序列化: " + json);
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JACKSON.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());
    }

    @Test
    @DisplayName("泛型 List / Map 支持")
    void genericCollection() {
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938},{\"name\":\"Jerry\",\"birthYear\":1940}]";
        String mapJson = "{\"cat\":{\"name\":\"Tom\",\"birthYear\":1938}}";

        List<Animal> list = JACKSON.fromJsonList(listJson, Animal.class);
        assertEquals("Tom", list.get(0).getName());
        assertEquals("Jerry", list.get(1).getName());

        Map<String, Object> map = JACKSON.fromJsonMap(mapJson);
        assertEquals("Tom", ((Map<?, ?>) map.get("cat")).get("name"));
    }

    // endregion

    // region 2. 注解翻译层

    @Test
    @DisplayName("@JsonField 字段重命名：ename 双向翻译")
    void fieldRename() {
        String json = JACKSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("序列化: " + json);
        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON 应使用注解名 ename");
        assertFalse(json.contains("\"name\""), "不应输出 Java 字段名 name");

        Employee back = JACKSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("@JsonField serialize=false：不输出但可接收")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0);

        String json = JACKSON.toJson(e);
        System.out.println("序列化: " + json);
        assertFalse(json.contains("cost"), "serialize=false 字段不应输出");

        Employee back = JACKSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        assertEquals(8000.0, back.getCost(), 0.001, "外部传入 cost 应被接收");
    }

    @Test
    @DisplayName("@JsonField deserialize=false：可输出但拒绝接收")
    void deserializeOff() {
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = JACKSON.fromJson(fromAccountant, Employee.class);
        System.out.println("反序列化后 profit（应忽略外部值）: " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false 应拒绝外部写入");
    }

    // endregion

    // region 3. 三库横向对比

    @Test
    @DisplayName("三库一致性：gson / fastjson / jackson 跑同一段业务代码")
    void crossLibraryConsistency() {
        JsonAdapter gson = new GsonJsonAdapter();
        JsonAdapter fastjson = new FastJsonJsonAdapter();

        Animal tom = new Animal("Tom", 1938);
        String json = "{\"name\":\"Tom\",\"birthYear\":1938}";

        for (JsonAdapter adapter : new JsonAdapter[]{gson, fastjson, JACKSON}) {
            String name = adapter.getClass().getSimpleName();
            Animal back = adapter.fromJson(json, Animal.class);
            System.out.println("[" + name + "] " + back.getName() + " / " + back.getBirthYear());
            assertEquals("Tom", back.getName(), name + " 反序列化 name 一致");
            assertEquals(1938, back.getBirthYear(), name + " 反序列化 birthYear 一致");
        }
        System.out.println("✅ gson / fastjson / jackson 三库业务代码完全一致");
    }

    // endregion
}
