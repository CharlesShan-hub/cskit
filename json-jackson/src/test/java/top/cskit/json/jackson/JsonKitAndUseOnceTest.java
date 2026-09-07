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
 * JsonKit 四种重载 + 一次性适配器测试
 * <p>
 * 验证统一方法名 + 重载区分使用模式的设计：
 * ① 默认适配器（configureDefault 可配置，对齐 YshJson 的 DEFAULT_GSON）
 * ② 模板名（走注册表）
 * ③ 适配器实例（一次性）
 * ④ 适配器工厂（一次性 + 定制）
 * 以及注册表 useOnce / use 一次性能力。
 */
class JsonKitAndUseOnceTest {

    @BeforeEach
    void setUp() {
        // 确保 gson 已注册（幂等），配置默认适配器
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
        // 清理可能残留的临时模板（仅当存在时删除，幂等）
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        if (registry.contains("tmp-once")) {
            registry.useOnce("tmp-once");
        }
        if (registry.contains("tmp-scoped")) {
            registry.useOnce("tmp-scoped");
        }
    }

    // ==================== ① 默认适配器 ====================

    @Test
    @DisplayName("① 默认适配器：configureDefault 后无参 toJson/fromJson 生效")
    void defaultAdapter() {
        String json = JsonKit.toJson(new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JsonKit.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());
        System.out.println("✅ 默认适配器: " + json);

        // 可随时切换默认（对齐 YshJson DEFAULT_GSON 但更灵活）
        JsonKit.configureDefault(new GsonJsonAdapter());
        assertTrue(JsonKit.hasDefault());
        System.out.println("✅ 默认适配器可切换");
    }

    @Test
    @DisplayName("① 未配置默认时抛异常（fail-fast）")
    void noDefaultThrows() throws Exception {
        // 用反射临时清空 defaultAdapter，验证 fail-fast 设计
        java.lang.reflect.Field field = JsonKit.class.getDeclaredField("defaultAdapter");
        field.setAccessible(true);
        JsonAdapter backup = (JsonAdapter) field.get(null);
        field.set(null, null);
        try {
            assertThrows(IllegalStateException.class, () -> JsonKit.toJson(new Animal("Tom", 1938)),
                    "未配置默认适配器应抛 IllegalStateException");
            System.out.println("✅ 未配置默认 → 抛异常（fail-fast）");
        } finally {
            field.set(null, backup); // 恢复
        }
    }

    // ==================== ② 模板名 ====================

    @Test
    @DisplayName("② 模板名：走注册表长期复用")
    void byTemplateName() {
        String json = JsonKit.toJson("gson", new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson("gson", json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("✅ 模板名重载: " + json);
    }

    // ==================== ③ 适配器实例（一次性） ====================

    @Test
    @DisplayName("③ 适配器实例：直接传，不注册不缓存")
    void byAdapterInstance() {
        String json = JsonKit.toJson(new GsonJsonAdapter(), new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson(new GsonJsonAdapter(), json, Animal.class);
        assertEquals("Tom", back.getName());

        // 证明没有匿名模板残留
        boolean leaked = JsonAdapterRegistry.getInstance().contains("anonymous");
        assertTrue(!leaked);
        System.out.println("✅ 实例重载（一次性）: " + json + "，未污染注册表");
    }

    // ==================== ④ 适配器工厂（一次性 + 定制） ====================

    @Test
    @DisplayName("④ 适配器工厂：每次 new 即弃，支持定制（对齐 YshJson 定制思想）")
    void byFactory() {
        String json = JsonKit.toJson(
                () -> new GsonJsonAdapter(builder -> builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
                new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        Animal back = JsonKit.fromJson(() -> new GsonJsonAdapter(), json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("✅ 工厂重载（一次性 + 定制）: " + json);
    }

    // ==================== 注册表一次性能力 ====================

    @Test
    @DisplayName("useOnce：取出即删，用完不污染全局")
    void useOnce() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("tmp-once", new GsonJsonAdapter());

        JsonAdapter tmp = registry.useOnce("tmp-once");
        String json = tmp.toJson(new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-once"));
        System.out.println("✅ useOnce: 取出即删，再次 get 抛异常");
    }

    @Test
    @DisplayName("use：作用域封闭，适配器只在 lambda 内有效")
    void useScoped() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("tmp-scoped", new GsonJsonAdapter());

        String json = registry.use("tmp-scoped", adapter -> adapter.toJson(new Animal("Jerry", 1940)));
        assertTrue(json.contains("\"name\":\"Jerry\""));

        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-scoped"));
        System.out.println("✅ use: 作用域封闭，执行完自动移除");
    }
}
