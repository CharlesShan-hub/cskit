package top.cskit.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Global registry of JSON adapters, keyed by template name.
 *
 * <p>Singleton by design (like Spring container / JDBC DriverManager): one
 * registry, one map, so caches stay meaningful. Usage modes:
 * <ul>
 *   <li><b>Long-lived</b>: {@link #register} + {@link #get} (e.g. shared gson/fastjson adapters)</li>
 *   <li><b>One-shot</b>: {@link #useOnce} (take and remove) or {@link #use} (scoped, auto-remove)</li>
 *   <li><b>Ephemeral</b>: skip registration, new an adapter and drop it ({@link JsonKit})</li>
 * </ul>
 * Example:
 * <pre>
 * // Long-lived: register + get
 * JsonAdapterRegistry.getInstance().register("gson", new GsonJsonAdapter());
 * JsonAdapter gson = JsonAdapterRegistry.getInstance().get("gson");
 *
 * // One-shot: take and remove
 * JsonAdapterRegistry.getInstance().register("tmp", new GsonJsonAdapter());
 * JsonAdapter tmp = JsonAdapterRegistry.getInstance().useOnce("tmp");
 *
 * // One-shot: scoped (auto-removed after the lambda)
 * String json = JsonAdapterRegistry.getInstance()
 *         .use("tmp", adapter -> adapter.toJson(obj));
 * </pre>
 * Adding a new backend (e.g. Jackson) only requires implementing {@link JsonAdapter}
 * and registering it; business code stays untouched.
 *
 * <p>Thread-safe (globally shared singleton):
 * <ul>
 *   <li>Eager singleton: created at class load, initialized once by the JVM;</li>
 *   <li>{@link ConcurrentHashMap}: register/get/contains/useOnce/use are atomic;</li>
 *   <li>Same-name concurrent registration: last write wins; concurrent useOnce: only one wins.</li>
 * </ul>
 * Concurrency verified by {@code JsonAdapterRegistryConcurrencyTest}.
 */
public final class JsonAdapterRegistry {

    /** Eager singleton instance. */
    private static final JsonAdapterRegistry INSTANCE = new JsonAdapterRegistry();

    private final Map<String, JsonAdapter> registry = new ConcurrentHashMap<>();

    /** Private constructor: singleton only. */
    private JsonAdapterRegistry() {
    }

    /** Returns the global singleton instance. */
    public static JsonAdapterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an adapter under a template name (chainable).
     *
     * @param name    template name, e.g. "gson", "fastjson"
     * @param adapter adapter implementation
     * @return this, for chaining
     */
    public JsonAdapterRegistry register(String name, JsonAdapter adapter) {
        registry.put(name, adapter);
        return this;
    }

    /**
     * Gets an adapter by template name; throws if not registered (fail-fast).
     *
     * @param name template name
     * @return the adapter
     */
    public JsonAdapter get(String name) {
        JsonAdapter adapter = registry.get(name);
        if (adapter == null) {
            throw new IllegalArgumentException("Unregistered JSON adapter: " + name);
        }
        return adapter;
    }

    /** Returns whether a template name is registered. */
    public boolean contains(String name) {
        return registry.containsKey(name);
    }

    /**
     * Takes an adapter once: removes it from the registry and returns it.
     * A temporary template should disappear after one use; concurrent
     * callers on the same name race on the atomic remove, only one wins.
     *
     * @param name template name
     * @return the adapter (already removed from the registry)
     */
    public JsonAdapter useOnce(String name) {
        JsonAdapter adapter = registry.remove(name);
        if (adapter == null) {
            throw new IllegalArgumentException("Unregistered JSON adapter: " + name);
        }
        return adapter;
    }

    /**
     * Uses an adapter in a scoped lambda, then removes it automatically.
     * Safer than {@link #useOnce}: the adapter is only visible inside the
     * function, so it cannot be left behind.
     *
     * @param name     template name
     * @param consumer function receiving the adapter
     * @param <T>      return type
     * @return the consumer's result
     */
    public <T> T use(String name, Function<JsonAdapter, T> consumer) {
        JsonAdapter adapter = useOnce(name);
        return consumer.apply(adapter);
    }
}
