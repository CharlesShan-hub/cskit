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
 * Tests for the Fastjson annotation translation layer: {@code @JsonField}
 * takes effect on fastjson2, consistent with Gson / Jackson (one annotation
 * set, three backends). Entity reused from {@link Employee}.
 */
class FastJsonAnnotationTest {

    static final JsonAdapter FASTJSON = new FastJsonJsonAdapter();

    @Test
    @DisplayName("Field rename: @JsonField(\"ename\") translated in both directions")
    void fieldRename() {
        String json = FASTJSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("serialized: " + json);
        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON should use the annotation name ename");
        assertFalse(json.contains("\"name\""), "Java field name should not be output");

        Employee back = FASTJSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        System.out.println("deserialized: name=" + back.getName() + ", salary=" + back.getSalary());
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("Serialize switch: serialize=false not output but still read")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0);

        String json = FASTJSON.toJson(e);
        System.out.println("serialized: " + json);
        assertFalse(json.contains("cost"), "serialize=false field must not be output");

        Employee back = FASTJSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        assertEquals(8000.0, back.getCost(), 0.001, "external cost should be accepted");
    }

    @Test
    @DisplayName("Deserialize switch: deserialize=false rejects external writes")
    void deserializeOff() {
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = FASTJSON.fromJson(fromAccountant, Employee.class);
        System.out.println("profit after deserialize (external value ignored): " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false must reject external writes");
    }

    @Test
    @DisplayName("Cross-library consistency: same @JsonField behaves identically on gson/fastjson/jackson")
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
            assertEquals("Tom", e.getName(), name + " should read ename");
            assertEquals(8000.5, e.getSalary(), 0.001, name + " salary consistent");
            assertEquals(999.0, e.getCost(), 0.001, name + " cost should be accepted");
            assertEquals(0.0, e.getProfit(), 0.001, name + " profit should reject external value");
        }
        System.out.println("one @JsonField, three backends (gson/fastjson/jackson) fully consistent");
    }
}
