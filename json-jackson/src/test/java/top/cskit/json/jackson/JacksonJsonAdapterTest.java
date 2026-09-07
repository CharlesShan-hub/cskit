package top.cskit.json.jackson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Animal;
import top.cskit.json.testkit.Employee;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jackson adapter tests: functionality + annotation translation layer.
 *
 * <p>Cross-library comparison: the same entity and the same business code on
 * gson / fastjson / jackson produce identical results. Entities reused from
 * {@link Animal} / {@link Employee}.
 */
class JacksonJsonAdapterTest {

    static final JsonAdapter JACKSON = new JacksonJsonAdapter();

    // region 1. Basic functionality

    @Test
    @DisplayName("Round trip: serialize + deserialize")
    void roundTrip() {
        String json = JACKSON.toJson(new Animal("Tom", 1938));
        System.out.println("serialized: " + json);
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JACKSON.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());
    }

    @Test
    @DisplayName("Generic List / Map support")
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

    // region 2. Annotation translation layer

    @Test
    @DisplayName("@JsonField rename: ename translated in both directions")
    void fieldRename() {
        String json = JACKSON.toJson(new Employee("Jack", 10000.5));
        System.out.println("serialized: " + json);
        assertTrue(json.contains("\"ename\":\"Jack\""), "JSON should use the annotation name ename");
        assertFalse(json.contains("\"name\""), "Java field name should not be output");

        Employee back = JACKSON.fromJson("{\"ename\":\"Tom\",\"salary\":8000.5}", Employee.class);
        assertEquals("Tom", back.getName());
        assertEquals(8000.5, back.getSalary(), 0.001);
    }

    @Test
    @DisplayName("@JsonField serialize=false: not output but still read")
    void serializeOff() {
        Employee e = new Employee("Jack", 10000.5);
        e.setCost(8000.0);

        String json = JACKSON.toJson(e);
        System.out.println("serialized: " + json);
        assertFalse(json.contains("cost"), "serialize=false field must not be output");

        Employee back = JACKSON.fromJson("{\"ename\":\"Jack\",\"salary\":10000.5,\"cost\":8000.0}", Employee.class);
        assertEquals(8000.0, back.getCost(), 0.001, "external cost should be accepted");
    }

    @Test
    @DisplayName("@JsonField deserialize=false: output but not read")
    void deserializeOff() {
        String fromAccountant = "{\"ename\":\"Boss\",\"salary\":100000.0,\"profit\":99999.0}";
        Employee boss = JACKSON.fromJson(fromAccountant, Employee.class);
        System.out.println("profit after deserialize (external value ignored): " + boss.getProfit());
        assertEquals(0.0, boss.getProfit(), 0.001, "deserialize=false must reject external writes");
    }

    // endregion

    // region 3. Cross-library comparison

    @Test
    @DisplayName("Cross-library consistency: same business code on gson/fastjson/jackson")
    void crossLibraryConsistency() {
        JsonAdapter gson = new GsonJsonAdapter();
        JsonAdapter fastjson = new FastJsonJsonAdapter();

        Animal tom = new Animal("Tom", 1938);
        String json = "{\"name\":\"Tom\",\"birthYear\":1938}";

        for (JsonAdapter adapter : new JsonAdapter[]{gson, fastjson, JACKSON}) {
            String name = adapter.getClass().getSimpleName();
            Animal back = adapter.fromJson(json, Animal.class);
            System.out.println("[" + name + "] " + back.getName() + " / " + back.getBirthYear());
            assertEquals("Tom", back.getName(), name + " deserialized name consistent");
            assertEquals(1938, back.getBirthYear(), name + " deserialized birthYear consistent");
        }
        System.out.println("gson / fastjson / jackson fully consistent for the same business code");
    }

    // endregion
}
