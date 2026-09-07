package top.cskit.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * JSON 适配器注册表（单例）：按模板名注册/选择底层实现。
 * <p>
 * 注册表本质是<b>全局注册中心</b>（类比 Spring 容器 / JDBC DriverManager），
 * 因此设计为全局唯一实例——缓存 JSON 构造器的表只有一张，避免各处
 * new 出多个注册表各注册各的，导致缓存失去意义。
 * <p>
 * 三种使用模式：
 * <ul>
 *   <li><b>长期复用</b>：{@link #register} + {@link #get}（如 gson / fastjson 常用适配器）</li>
 *   <li><b>一次性使用</b>：{@link #useOnce}（取出即删）或 {@link #use}（作用域封闭）——
 *       临时模板用完即弃，不污染全局（对齐 YshJson 定制路径"用完即弃"思想）</li>
 *   <li><b>即用即弃</b>：不注册，直接 new 适配器用完就扔（{@link JsonKit} 提供静态便捷入口）</li>
 * </ul>
 * 用法：
 * <pre>
 * // 长期：注册 + 取用
 * JsonAdapterRegistry.getInstance()
 *         .register("gson", new GsonJsonAdapter());
 * JsonAdapter gson = JsonAdapterRegistry.getInstance().get("gson");
 *
 * // 一次性：取出即删
 * JsonAdapterRegistry.getInstance().register("tmp", new GsonJsonAdapter());
 * JsonAdapter tmp = JsonAdapterRegistry.getInstance().useOnce("tmp");
 *
 * // 一次性：作用域封闭（执行完自动移除）
 * String json = JsonAdapterRegistry.getInstance()
 *         .use("tmp", adapter -> adapter.toJson(obj));
 * </pre>
 * 新增底层库（Jackson 等）只需实现 {@link JsonAdapter} 并注册，业务代码零改动。
 * <p>
 * <b>线程安全</b>（单例全局共享，必须并发安全）：
 * <ul>
 *   <li>饿汉式单例：类加载时创建，JVM 保证只初始化一次；</li>
 *   <li>内部 {@link ConcurrentHashMap}：register / get / contains / useOnce / use 均为原子操作；</li>
 *   <li>并发注册同名模板为"后写覆盖"；并发 useOnce 同名模板只有一个能取到。</li>
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

    /**
     * 一次性取用：取出即从注册表移除（用完即弃，不污染全局）。
     * <p>
     * 对齐 YshJson 定制路径"每次 new、用完即弃"的思想——临时模板用一次就该消失。
     * 并发下同名模板只有一个线程能取到（remove 原子操作）。
     *
     * @param name 模板名
     * @return 适配器（已从注册表移除）
     */
    public JsonAdapter useOnce(String name) {
        JsonAdapter adapter = registry.remove(name);
        if (adapter == null) {
            throw new IllegalArgumentException("未注册的 JSON 适配器: " + name);
        }
        return adapter;
    }

    /**
     * 一次性使用（作用域封闭）：取出适配器执行 consumer，执行后自动从注册表移除。
     * <p>
     * 比 {@link #useOnce} 更安全：适配器只在 lambda 内可见，防止漏删。
     *
     * @param name     模板名
     * @param consumer 使用函数（适配器仅在函数内有效）
     * @param <T>      返回值类型
     * @return consumer 的返回值
     */
    public <T> T use(String name, Function<JsonAdapter, T> consumer) {
        JsonAdapter adapter = useOnce(name);
        return consumer.apply(adapter);
    }
}
