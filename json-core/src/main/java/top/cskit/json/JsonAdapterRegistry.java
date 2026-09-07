package top.cskit.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON 适配器注册表（单例）：按模板名注册/选择底层实现。
 * <p>
 * 注册表本质是<b>全局注册中心</b>（类比 Spring 容器 / JDBC DriverManager），
 * 因此设计为全局唯一实例——缓存 JSON 构造器的表只有一张，避免各处
 * new 出多个注册表各注册各的，导致缓存失去意义。
 * <p>
 * 用法：
 * <pre>
 * JsonAdapterRegistry.getInstance()
 *         .register("gson", new GsonJsonAdapter())
 *         .register("fastjson", new FastJsonJsonAdapter());
 *
 * JsonAdapter adapter = JsonAdapterRegistry.getInstance().get("fastjson"); // 模板选择
 * String json = adapter.toJson(obj);
 * </pre>
 * 新增底层库（Jackson 等）只需实现 {@link JsonAdapter} 并注册，业务代码零改动。
 * <p>
 * <b>线程安全</b>（单例全局共享，必须并发安全）：
 * <ul>
 *   <li>饿汉式单例：类加载时创建，JVM 保证只初始化一次；</li>
 *   <li>内部 {@link ConcurrentHashMap}：register / get / contains 均为原子操作；</li>
 *   <li>并发注册同名模板为"后写覆盖"，不抛异常、不产生脏数据。</li>
 * </ul>
 * 并发安全性已由 {@code JsonAdapterRegistryConcurrencyTest} 压力验证。
 */
public final class JsonAdapterRegistry {

    /** 全局唯一实例（饿汉式，类加载即创建，线程安全） */
    private static final JsonAdapterRegistry INSTANCE = new JsonAdapterRegistry();

    private final Map<String, JsonAdapter> registry = new ConcurrentHashMap<>();

    /** 私有构造器：禁止外部 new（单例） */
    private JsonAdapterRegistry() {
    }

    /**
     * 获取全局唯一注册表实例
     *
     * @return 单例
     */
    public static JsonAdapterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册适配器，链式
     *
     * @param name    模板名（如 "gson"、"fastjson"）
     * @param adapter 适配器实现
     * @return this（链式调用）
     */
    public JsonAdapterRegistry register(String name, JsonAdapter adapter) {
        registry.put(name, adapter);
        return this;
    }

    /**
     * 按模板名取适配器，未注册抛异常（fail-fast 防手滑）
     *
     * @param name 模板名
     * @return 适配器
     */
    public JsonAdapter get(String name) {
        JsonAdapter adapter = registry.get(name);
        if (adapter == null) {
            throw new IllegalArgumentException("未注册的 JSON 适配器: " + name);
        }
        return adapter;
    }

    /**
     * 是否存在
     *
     * @param name 模板名
     * @return true 表示已注册
     */
    public boolean contains(String name) {
        return registry.containsKey(name);
    }
}
