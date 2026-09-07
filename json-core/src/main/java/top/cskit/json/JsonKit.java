package top.cskit.json;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Static convenience entry for JSON operations.
 *
 * <p>One method name, overloads distinguish usage modes:
 * <pre>
 * // 1) default adapter (set via configureDefault)
 * JsonKit.configureDefault(new GsonJsonAdapter());
 * String j1 = JsonKit.toJson(obj);
 *
 * // 2) template name (a registered adapter, reused)
 * String j2 = JsonKit.toJson("gson", obj);
 *
 * // 3) adapter instance (one-shot: not registered, not cached)
 * String j3 = JsonKit.toJson(new GsonJsonAdapter(), obj);
 *
 * // 4) adapter factory (one-shot + customization)
 * String j4 = JsonKit.toJson(
 *         () -> new GsonJsonAdapter(builder -> builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
 *         obj);
 * </pre>
 */
public final class JsonKit {

    private JsonKit() {
    }

    /** Default adapter; volatile for cross-thread visibility. Fails fast if not set. */
    private static volatile JsonAdapter defaultAdapter;

    /**
     * Sets the default adapter used by the no-arg overloads.
     *
     * @param adapter default adapter (must not be null)
     */
    public static void configureDefault(JsonAdapter adapter) {
        defaultAdapter = Objects.requireNonNull(adapter, "default adapter must not be null");
    }

    /** Returns whether a default adapter has been configured. */
    public static boolean hasDefault() {
        return defaultAdapter != null;
    }

    // ==================== Serialization: toJson (4 overloads) ====================

    /** 1) default adapter */
    public static String toJson(Object obj) {
        return defaultAdapter().toJson(obj);
    }

    /** 2) template name (from registry) */
    public static String toJson(String templateName, Object obj) {
        return registry().get(templateName).toJson(obj);
    }

    /** 3) adapter instance (one-shot) */
    public static String toJson(JsonAdapter adapter, Object obj) {
        return adapter.toJson(obj);
    }

    /** 4) adapter factory (one-shot + customization) */
    public static String toJson(Supplier<JsonAdapter> factory, Object obj) {
        return factory.get().toJson(obj);
    }

    // ==================== Deserialization: fromJson (4 overloads) ====================

    /** 1) default adapter */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return defaultAdapter().fromJson(json, classOfT);
    }

    /** 2) template name (from registry) */
    public static <T> T fromJson(String templateName, String json, Class<T> classOfT) {
        return registry().get(templateName).fromJson(json, classOfT);
    }

    /** 3) adapter instance (one-shot) */
    public static <T> T fromJson(JsonAdapter adapter, String json, Class<T> classOfT) {
        return adapter.fromJson(json, classOfT);
    }

    /** 4) adapter factory (one-shot + customization) */
    public static <T> T fromJson(Supplier<JsonAdapter> factory, String json, Class<T> classOfT) {
        return factory.get().fromJson(json, classOfT);
    }

    // ==================== Internal ====================

    private static JsonAdapter defaultAdapter() {
        JsonAdapter adapter = defaultAdapter;
        if (adapter == null) {
            throw new IllegalStateException("No default adapter configured; call JsonKit.configureDefault(adapter) first");
        }
        return adapter;
    }

    private static JsonAdapterRegistry registry() {
        return JsonAdapterRegistry.getInstance();
    }
}
