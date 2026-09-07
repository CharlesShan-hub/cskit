package top.cskit.json.jackson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.JsonKit;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Animal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JsonKit's four overloads and one-shot registry adapters.
 *
 * <p>Verifies the design of one method name with overloads distinguishing
 * usage modes:
 * <ol>
 *   <li>default adapter (configurable via configureDefault)</li>
 *   <li>template name (from registry)</li>
 *   <li>adapter instance (one-shot)</li>
 *   <li>adapter factory (one-shot + customization)</li>
 * </ol>
 * Plus the registry's useOnce / use one-shot capabilities.
 */
class JsonKitAndUseOnceTest {

    @BeforeEach
    void setUp() {
        // Ensure gson is registered (idempotent) and the default adapter is set
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        if (!registry.contains("gson")) {
            registry.register("gson", new GsonJsonAdapter());
        }
        if (!JsonKit.hasDefault()) {
            JsonKit.configureDefault(new GsonJsonAdapter());
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up possible leftover temporary templates (idempotent)
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        if (registry.contains("tmp-once")) {
            registry.useOnce("tmp-once");
        }
        if (registry.contains("tmp-scoped")) {
            registry.useOnce("tmp-scoped");
        }
    }

    // ==================== 1) Default adapter ====================

    @Test
    @DisplayName("1) Default adapter: no-arg toJson/fromJson work after configureDefault")
    void defaultAdapter() {
        String json = JsonKit.toJson(new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JsonKit.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());
        System.out.println("default adapter: " + json);

        // Default is switchable at any time
        JsonKit.configureDefault(new GsonJsonAdapter());
        assertTrue(JsonKit.hasDefault());
        System.out.println("default adapter is switchable");
    }

    @Test
    @DisplayName("1) No default configured throws (fail-fast)")
    void noDefaultThrows() throws Exception {
        // Temporarily clear defaultAdapter via reflection to verify fail-fast
        java.lang.reflect.Field field = JsonKit.class.getDeclaredField("defaultAdapter");
        field.setAccessible(true);
        JsonAdapter backup = (JsonAdapter) field.get(null);
        field.set(null, null);
        try {
            assertThrows(IllegalStateException.class, () -> JsonKit.toJson(new Animal("Tom", 1938)),
                    "no default adapter should throw IllegalStateException");
            System.out.println("no default -> throws (fail-fast)");
        } finally {
            field.set(null, backup); // restore
        }
    }

    // ==================== 2) Template name ====================

    @Test
    @DisplayName("2) Template name: long-lived reuse via registry")
    void byTemplateName() {
        String json = JsonKit.toJson("gson", new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson("gson", json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("template name overload: " + json);
    }

    // ==================== 3) Adapter instance (one-shot) ====================

    @Test
    @DisplayName("3) Adapter instance: passed directly, not registered or cached")
    void byAdapterInstance() {
        String json = JsonKit.toJson(new GsonJsonAdapter(), new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson(new GsonJsonAdapter(), json, Animal.class);
        assertEquals("Tom", back.getName());

        // Prove no anonymous template leaked into the registry
        boolean leaked = JsonAdapterRegistry.getInstance().contains("anonymous");
        assertTrue(!leaked);
        System.out.println("instance overload (one-shot): " + json + ", registry not polluted");
    }

    // ==================== 4) Adapter factory (one-shot + customization) ====================

    @Test
    @DisplayName("4) Adapter factory: new-and-drop each time, supports customization")
    void byFactory() {
        String json = JsonKit.toJson(
                () -> new GsonJsonAdapter(builder -> builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
                new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JsonKit.fromJson(() -> new GsonJsonAdapter(), json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("factory overload (one-shot + customization): " + json);
    }

    // ==================== Registry one-shot capabilities ====================

    @Test
    @DisplayName("useOnce: take and remove, no pollution")
    void useOnce() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("tmp-once", new GsonJsonAdapter());

        JsonAdapter tmp = registry.useOnce("tmp-once");
        String json = tmp.toJson(new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-once"));
        System.out.println("useOnce: removed after take, next get throws");
    }

    @Test
    @DisplayName("use: scoped, adapter valid only inside the lambda")
    void useScoped() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("tmp-scoped", new GsonJsonAdapter());

        String json = registry.use("tmp-scoped", adapter -> adapter.toJson(new Animal("Jerry", 1940)));
        assertTrue(json.contains("\"name\":\"Jerry\""));

        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-scoped"));
        System.out.println("use: scoped, auto-removed after execution");
    }
}
