package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the framework {@code @JsonField} annotation on the Gson
 * translation layer. Reuses {@link Employee} (shared with the fastjson /
 * jackson annotation tests):
 * <ol>
 *   <li>Field rename: {@code @JsonField("ename")} -> JSON outputs "ename"</li>
 *   <li>Serialize switch: {@code @JsonField(serialize=false)} -> not output</li>
 *   <li>Deserialize switch: {@code @JsonField(deserialize=false)} -> not read</li>
 *   <li>Round trip: renamed field survives serialize -> deserialize</li>
 * </ol>
 */
class JsonFieldAnnotationTest {

    static final JsonAdapter GSON = new GsonJsonAdapter();

    @Test
    @DisplayName("Field rename: @JsonField(\"ename\") used for both serialize and deserialize")
    void fieldRename() {
        String json = GSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("serialized: " + json);

        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON should use the annotation name ename");
        assertFalse(json.contains("\"name\""), "Java field name should not be output");

        // Deserialize: ename -> name
        Employee back = GSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        System.out.println("deserialized: name=" + back.getName() + ", salary=" + back.getSalary());
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("Serialize switch: @JsonField(serialize=false) not output, but still read")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0); // accountant-filled cost must not be sent out

        String json = GSON.toJson(e);
        System.out.println("serialized: " + json);
        assertFalse(json.contains("cost"), "serialize=false field must not be output");
        assertTrue(json.contains("\"ename\""), "ordinary fields still output");

        // cost present in JSON should be accepted back
        Employee back = GSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        System.out.println("received cost=" + back.getCost());
        assertEquals(8000.0, back.getCost(), 0.001);
    }

    @Test
    @DisplayName("Deserialize switch: @JsonField(deserialize=false) output but not read")
    void deserializeOff() {
        // Boss serialization still outputs profit (possibly 0, computed by getter)
        String json = GSON.toJson(new Employee("Boss", 100000.0));
        System.out.println("serialized: " + json);

        // Accountant sends profit back, but boss does not accept external writes
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = GSON.fromJson(fromAccountant, Employee.class);
        System.out.println("profit after deserialize (external value ignored): " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false must reject external writes");
    }

    @Test
    @DisplayName("Round trip: renamed field survives serialize -> deserialize without data loss")
    void roundTrip() {
        Employee e = new Employee("Jerry", 9000.5);
        String json = GSON.toJson(e);
        Employee back = GSON.fromJson(json, Employee.class);
        assertEquals("Jerry", back.getName());
        assertEquals(9000.5, back.getSalary(), 0.001);
    }
}
