package top.cskit.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 统一 JSON 适配器接口（框架门面）
 * <p>
 * 业务代码只依赖本接口，底层实现（Gson / Fastjson / Jackson...）可随时替换，
 * 通过 {@link JsonAdapterRegistry} 注册 + 模板名选择，实现"换库零改动"。
 */
public interface JsonAdapter {

    /**
     * 序列化为 JSON 字符串
     *
     * @param obj 任意对象
     * @return JSON 字符串
     */
    String toJson(Object obj);

    /**
     * 反序列化为对象（非泛型）
     *
     * @param json     JSON 字符串
     * @param classOfT 目标类型
     * @param <T>      泛型
     * @return 对象实例
     */
    <T> T fromJson(String json, Class<T> classOfT);

    /**
     * 反序列化为对象（可泛型，如 List&lt;T&gt; / Map&lt;String, T&gt;）
     *
     * @param json JSON 字符串
     * @param type 真实类型（TypeToken 获取）
     * @param <T>  泛型
     * @return 对象实例
     */
    <T> T fromJson(String json, Type type);

    /**
     * 反序列化为 List
     *
     * @param json     JSON 字符串
     * @param classOfT 元素类型
     * @param <T>      元素泛型
     * @return List 实例
     */
    <T> List<T> fromJsonList(String json, Class<T> classOfT);

    /**
     * 反序列化为 Map
     *
     * @param json JSON 字符串
     * @return Map 实例
     */
    Map<String, Object> fromJsonMap(String json);
}
