package top.cskit.json;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * JSON 静态便捷入口（对标 YshJson 的 toJsonString / fromJsonString）。
 * <p>
 * 统一方法名 + 重载区分使用模式，避免一堆不同名字的 API：
 * <pre>
 * // ① 默认适配器（configureDefault 设置，对齐 YshJson 的 DEFAULT_GSON）
 * JsonKit.configureDefault(new GsonJsonAdapter());
 * String j1 = JsonKit.toJson(obj);
 *
 * // ② 模板名（走注册表已注册的适配器，长期复用）
 * String j2 = JsonKit.toJson("gson", obj);
 *
 * // ③ 适配器实例（一次性：不注册、不缓存）
 * String j3 = JsonKit.toJson(new GsonJsonAdapter(), obj);
 *
 * // ④ 适配器工厂（一次性 + 定制：每次 new 即弃，对齐 YshJson 定制路径）
 * String j4 = JsonKit.toJson(
 *         () -&gt; new GsonJsonAdapter(builder -&gt; builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
 *         obj);
 * </pre>
 */
public final class JsonKit {

    private JsonKit() {
    }

    /** 默认适配器（volatile：跨线程可见性；未设置时 get 抛异常 fail-fast） */
    private static volatile JsonAdapter defaultAdapter;

    /**
     * 配置默认适配器（对齐 YshJson 的 DEFAULT_GSON，但可随时换成任意适配器）。
     * 之后无参 {@link #toJson(Object)} / {@link #fromJson(String, Class)} 走它。
     *
     * @param adapter 默认适配器（不可为 null）
     */
    public static void configureDefault(JsonAdapter adapter) {
        defaultAdapter = Objects.requireNonNull(adapter, "默认适配器不能为 null");
    }

    /** 默认适配器是否已配置 */
    public static boolean hasDefault() {
        return defaultAdapter != null;
    }

    // ==================== 序列化 toJson（四种重载） ====================

    /** ① 默认适配器 */
    public static String toJson(Object obj) {
        return defaultAdapter().toJson(obj);
    }

    /** ② 模板名（走注册表） */
    public static String toJson(String templateName, Object obj) {
        return registry().get(templateName).toJson(obj);
    }

    /** ③ 适配器实例（一次性） */
    public static String toJson(JsonAdapter adapter, Object obj) {
        return adapter.toJson(obj);
    }

    /** ④ 适配器工厂（一次性 + 定制） */
    public static String toJson(Supplier<JsonAdapter> factory, Object obj) {
        return factory.get().toJson(obj);
    }

    // ==================== 反序列化 fromJson（四种重载） ====================

    /** ① 默认适配器 */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return defaultAdapter().fromJson(json, classOfT);
    }

    /** ② 模板名（走注册表） */
    public static <T> T fromJson(String templateName, String json, Class<T> classOfT) {
        return registry().get(templateName).fromJson(json, classOfT);
    }

    /** ③ 适配器实例（一次性） */
    public static <T> T fromJson(JsonAdapter adapter, String json, Class<T> classOfT) {
        return adapter.fromJson(json, classOfT);
    }

    /** ④ 适配器工厂（一次性 + 定制） */
    public static <T> T fromJson(Supplier<JsonAdapter> factory, String json, Class<T> classOfT) {
        return factory.get().fromJson(json, classOfT);
    }

    // ==================== 内部 ====================

    private static JsonAdapter defaultAdapter() {
        JsonAdapter adapter = defaultAdapter;
        if (adapter == null) {
            throw new IllegalStateException("尚未配置默认适配器，请先调用 JsonKit.configureDefault(adapter)");
        }
        return adapter;
    }

    private static JsonAdapterRegistry registry() {
        return JsonAdapterRegistry.getInstance();
    }
}
