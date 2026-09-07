package top.cskit.json;

import java.util.function.Supplier;

/**
 * JSON 静态便捷入口（对标 YshJson 的 toJsonString / fromJsonString）。
 * <p>
 * 两种用法：
 * <ul>
 *   <li><b>走注册表</b>：按模板名取已注册的适配器（长期复用）</li>
 *   <li><b>即用即弃</b>：直接传入适配器或适配器工厂，用完即弃、不缓存
 *       （对齐 YshJson 定制路径"每次 new、用完即弃"的思想）</li>
 * </ul>
 * <pre>
 * // 走注册表（需要先注册）
 * String json = JsonKit.toJson("gson", obj);
 *
 * // 即用即弃：直接给适配器（不注册、不缓存）
 * String json = JsonKit.toJson(new GsonJsonAdapter(), obj);
 *
 * // 即用即弃 + 定制：传工厂，内部 new 完即弃
 * String json = JsonKit.toJson(
 *         () -&gt; new GsonJsonAdapter(builder -&gt; builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
 *         obj);
 * </pre>
 */
public final class JsonKit {

    private JsonKit() {
    }

    // ==================== 走注册表（按模板名） ====================

    /** 按模板名序列化（适配器须已注册） */
    public static String toJson(String templateName, Object obj) {
        return registry().get(templateName).toJson(obj);
    }

    /** 按模板名反序列化（适配器须已注册） */
    public static <T> T fromJson(String templateName, String json, Class<T> classOfT) {
        return registry().get(templateName).fromJson(json, classOfT);
    }

    // ==================== 即用即弃：直接传适配器 ====================

    /** 用给定适配器序列化（不注册、不缓存，用完即弃） */
    public static String toJson(JsonAdapter adapter, Object obj) {
        return adapter.toJson(obj);
    }

    /** 用给定适配器反序列化（不注册、不缓存，用完即弃） */
    public static <T> T fromJson(JsonAdapter adapter, String json, Class<T> classOfT) {
        return adapter.fromJson(json, classOfT);
    }

    // ==================== 即用即弃 + 定制：传适配器工厂 ====================

    /**
     * 序列化：通过工厂创建适配器（内部 new 完即弃，不注册不缓存）。
     *
     * @param factory 适配器工厂（每次调用返回新实例）
     * @param obj     待序列化对象
     * @return JSON 字符串
     */
    public static String toJson(Supplier<JsonAdapter> factory, Object obj) {
        return factory.get().toJson(obj);
    }

    /**
     * 反序列化：通过工厂创建适配器（内部 new 完即弃，不注册不缓存）。
     *
     * @param factory  适配器工厂（每次调用返回新实例）
     * @param json     JSON 字符串
     * @param classOfT 目标类型
     * @param <T>      泛型
     * @return 对象实例
     */
    public static <T> T fromJson(Supplier<JsonAdapter> factory, String json, Class<T> classOfT) {
        return factory.get().fromJson(json, classOfT);
    }

    // ==================== 内部 ====================

    private static JsonAdapterRegistry registry() {
        return JsonAdapterRegistry.getInstance();
    }
}
