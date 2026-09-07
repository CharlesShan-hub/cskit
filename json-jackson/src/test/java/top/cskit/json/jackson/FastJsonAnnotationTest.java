package top.cskit.json.jackson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.jackson.JacksonJsonAdapter;
import top.cskit.json.testkit.Employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fastjson 注解翻译层测试：{@code @JsonField} 在 fastjson2 底层生效，
 * 且与 Gson / Jackson 结果一致（一套注解，三库统一）。
 * 实体复用 {@link Employee}（共享测试实体，三库共用同一个类证明注解统一）。
 */
class FastJsonAnnotationTest {

    static final JsonAdapter FASTJSON = new FastJsonJsonAdapter();

    @Test
    @DisplayName("字段重命名：@JsonField(\"ename\") 双向翻译")
    void fieldRename() {
        String json = FASTJSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("序列化: " + json);
        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON 应使用注解名 ename");
        assertFalse(json.contains("\"name\""), "不应输出 Java 字段名 name");

        Employee back = FASTJSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        System.out.println("反序列化: name=" + back.getName() + ", salary=" + back.getSalary());
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("序列化开关：serialize=false 不输出但可接收")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0);

        String json = FASTJSON.toJson(e);
        System.out.println("序列化: " + json);
        assertFalse(json.contains("cost"), "serialize=false 字段不应输出");

        Employee back = FASTJSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        assertEquals(8000.0, back.getCost(), 0.001, "外部传入 cost 应被接收");
    }

    @Test
    @DisplayName("反序列化开关：deserialize=false 拒绝外部写入")
    void deserializeOff() {
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = FASTJSON.fromJson(fromAccountant, Employee.class);
        System.out.println("反序列化后 profit（应忽略外部值）: " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false 应拒绝外部写入");
    }

    @Test
    @DisplayName("三库注解一致性：同一 @JsonField 在 gson / fastjson / jackson 行为一致")
    void crossLibraryAnnotationConsistency() {
        JsonAdapter gson = new GsonJsonAdapter();
        JsonAdapter jackson = new JacksonJsonAdapter();
        JsonAdapter fastjson = FASTJSON;

        String json = "{\"ename\":\"Tom\",\"salary\":8000.5,\"cost\":999.0,\"profit\":12345.0}";

        for (JsonAdapter adapter : new JsonAdapter[]{gson, jackson, fastjson}) {
            String name = adapter.getClass().getSimpleName();
            Employee e = adapter.fromJson(json, Employee.class);
            System.out.println("[" + name + "] name=" + e.getName()
                    + ", salary=" + e.getSalary()
                    + ", cost=" + e.getCost()
                    + ", profit=" + e.getProfit());
            assertEquals("Tom", e.getName(), name + " 应读取 ename");
            assertEquals(8000.5, e.getSalary(), 0.001, name + " salary 一致");
            assertEquals(999.0, e.getCost(), 0.001, name + " cost 应被接收");
            assertEquals(0.0, e.getProfit(), 0.001, name + " profit 应拒绝外部值");
        }
        System.out.println("✅ 一套 @JsonField，gson / fastjson / jackson 三库行为完全一致");
    }
}
