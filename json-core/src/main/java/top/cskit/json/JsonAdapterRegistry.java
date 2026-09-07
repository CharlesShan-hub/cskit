package top.cskit.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON 适配器注册表：按模板名注册/选择底层实现。
 * <p>
 * 用法：
 * <pre>
 * JsonAdapterRegistry registry = new JsonAdapterRegistry();
 * registry.register("gson", new GsonJsonAdapter())
 *          .register("fastjson", new FastJsonJsonAdapter());
 *
 * JsonAdapter adapter = registry.get("fastjson"); // 模板选择
 * String json = adapter.toJson(obj);
 * </pre>
 * 新增底层库（Jackson 等）只需实现 {@link JsonAdapter} 并注册，业务代码零改动。
 */
public class JsonAdapterRegistry {

    private final Map<String, JsonAdapter> registry = new ConcurrentHashMap<>();

    /** 注册适配器，链式 */
    public JsonAdapterRegistry register(String name, JsonAdapter adapter) {
        registry.put(name, adapter);
        return this;
    }

    /** 按模板名取适配器，未注册抛异常（fail-fast 防手滑） */
    public JsonAdapter get(String name) {
        JsonAdapter adapter = registry.get(name);
        if (adapter == null) {
            throw new IllegalArgumentException("未注册的 JSON 适配器: " + name);
        }
        return adapter;
    }

    /** 是否存在 */
    public boolean contains(String name) {
        return registry.containsKey(name);
    }
}
