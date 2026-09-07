package top.cskit.json.jackson;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Animal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adapter-pattern demo of the JSON framework.
 *
 * <p>Core idea: business code only sees the {@link JsonAdapter} interface;
 * the backend is chosen via registry + template name (gson / fastjson),
 * so swapping a backend requires zero code change. Entity reused from
 * {@link Animal}.
 */
class JsonAdapterDemoTest {

    /** Global registry: pre-register gson and fastjson templates. */
    static final JsonAdapterRegistry REGISTRY = JsonAdapterRegistry.getInstance()
            .register("gson", new GsonJsonAdapter())
            .register("fastjson", new FastJsonJsonAdapter());

    // region Core demo: same business code, two backends

    @Test
    @DisplayName("Template switch: same code runs on gson and fastjson")
    void templateSwitch() {
        Animal tom = new Animal("Tom", 1938);

        for (String template : new String[]{"gson", "fastjson"}) {
            JsonAdapter adapter = REGISTRY.get(template); // <- template chosen on this line

            // Serialize
            String json = adapter.toJson(tom);
            System.out.println("[" + template + "] toJson: " + json);
            assertTrue(json.contains("\"name\":\"Tom\""), template + " serialized result should contain name");

            // Deserialize
            Animal back = adapter.fromJson(json, Animal.class);
            System.out.println("[" + template + "] fromJson: " + back.getName() + " / " + back.getBirthYear());
            assertEquals("Tom", back.getName());
            assertEquals(1938, back.getBirthYear());
        }
    }

    @Test
    @DisplayName("Generics and collections: List / Map on both backends")
    void genericCollection() {
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938},{\"name\":\"Jerry\",\"birthYear\":1940}]";
        String mapJson = "{\"cat\":{\"name\":\"Tom\",\"birthYear\":1938}}";

        for (String template : new String[]{"gson", "fastjson"}) {
            JsonAdapter adapter = REGISTRY.get(template);

            List<Animal> list = adapter.fromJsonList(listJson, Animal.class);
            System.out.println("[" + template + "] List[0]: " + list.get(0).getName());
            assertEquals("Tom", list.get(0).getName());

            Map<String, Object> map = adapter.fromJsonMap(mapJson);
            System.out.println("[" + template + "] Map: " + map.keySet());
            assertEquals("Tom", ((Map<?, ?>) map.get("cat")).get("name"));
        }
    }

    // endregion

    // region Framework robustness

    @Test
    @DisplayName("Unknown template name throws (fail-fast)")
    void unknownTemplate() {
        assertThrows(IllegalArgumentException.class, () -> REGISTRY.get("jackson"));
    }

    @Test
    @DisplayName("Registry is a global singleton: getInstance always returns the same instance")
    void singleton() {
        JsonAdapterRegistry a = JsonAdapterRegistry.getInstance();
        JsonAdapterRegistry b = JsonAdapterRegistry.getInstance();
        System.out.println("same instance from two getInstance calls: " + (a == b));
        assertEquals(a, b, "singleton registry should return the same instance");

        // Singleton benefit: templates registered elsewhere are visible here
        assertTrue(a.contains("gson"));
        assertTrue(a.contains("fastjson"));
    }

    // endregion
}
