package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Animal;
import top.cskit.json.testkit.Tourist;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Named registry test ("eat our own dog food").
 *
 * <p>Verifies the named-registry design: register customized configs into
 * {@link JsonAdapterRegistry} by name, then fetch them by name — the registry
 * plus custom-constructed {@link GsonJsonAdapter}s bring the idea to life.
 * Entities reused from {@link Animal} / {@link Tourist}.
 */
class NamedGsonRegistryTest {

    /** Global registry: register a few named, customized Gson adapters. */
    static final JsonAdapterRegistry REGISTRY = JsonAdapterRegistry.getInstance()
            .register("pretty-date", new GsonJsonAdapter(builder ->
                    builder.setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting()))
            .register("null-keep", new GsonJsonAdapter(builder -> builder.serializeNulls()))
            .register("default", new GsonJsonAdapter());

    @Test
    @DisplayName("Named customization: chainable registration + reuse by name")
    void namedRegistry() {
        // 1) "pretty-date": date formatting + indentation
        String pretty = REGISTRY.get("pretty-date").toJson(new Tourist("Elsa", new Date(1710000000000L)));
        System.out.println("[pretty-date]\n" + pretty);
        assertTrue(pretty.contains("2024-03-10"));
        assertTrue(pretty.contains("\n")); // pretty printing has newlines

        // 2) "null-keep": null fields are kept
        String keepNull = REGISTRY.get("null-keep").toJson(new Animal(null, 1938));
        System.out.println("[null-keep] " + keepNull);
        assertTrue(keepNull.contains("\"name\":null"));

        // 3) "default": default behavior, null not output
        String defaultJson = REGISTRY.get("default").toJson(new Animal(null, 1938));
        System.out.println("[default] " + defaultJson);
        assertTrue(!defaultJson.contains("name"));

        // 4) Same name always returns the same registered instance (cache works)
        JsonAdapter first = REGISTRY.get("pretty-date");
        JsonAdapter second = REGISTRY.get("pretty-date");
        System.out.println("same instance for same name: " + (first == second));
        assertSame(first, second); // reference equality = cache hit

        // 5) Generics supported: deserialize a List by name
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938}]";
        List<Animal> animals = REGISTRY.get("default").fromJsonList(listJson, Animal.class);
        System.out.println("[default] generic deserialize: " + animals.get(0).getName());
        assertEquals("Tom", animals.get(0).getName());
    }

    @Test
    @DisplayName("Unknown name throws (fail-fast)")
    void unknownNameThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> REGISTRY.get("typo-name"));
        System.out.println("caught expected exception: " + e.getMessage());
        assertTrue(e.getMessage().contains("Unregistered"));
    }
}
