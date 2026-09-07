package top.cskit.json.jackson;

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
 * 一次性适配器 + JsonKit 静态便捷入口测试
 * <p>
 * 验证三种使用模式：
 * 1. 长期复用：register + get（已有）
 * 2. 一次性使用：useOnce（取出即删）/ use（作用域封闭）
 * 3. 即用即弃：JsonKit 静态入口（直接传适配器 / 工厂，不缓存）
 */
class JsonKitAndUseOnceTest {

    @Test
    @DisplayName("useOnce：取出即删，用完不污染全局")
    void useOnce() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();

        // 注册一个临时模板
        registry.register("tmp-once", new GsonJsonAdapter());

        // 取用即删
        JsonAdapter tmp = registry.useOnce("tmp-once");
        String json = tmp.toJson(new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        // 已从注册表移除 → 再取会抛"未注册"
        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-once"));
        System.out.println("✅ useOnce: 取出即删，再次 get 抛异常（未污染全局）");
    }

    @Test
    @DisplayName("use：作用域封闭，适配器只在 lambda 内有效")
    void useScoped() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();

        registry.register("tmp-scoped", new GsonJsonAdapter());

        // 作用域封闭：在 lambda 内使用，返回结果
        String json = registry.use("tmp-scoped", adapter -> adapter.toJson(new Animal("Jerry", 1940)));
        assertTrue(json.contains("\"name\":\"Jerry\""));

        // 执行完自动移除 → 再取会抛异常
        assertThrows(IllegalArgumentException.class, () -> registry.get("tmp-scoped"));
        System.out.println("✅ use: 作用域封闭，执行完自动移除");
    }

    @Test
    @DisplayName("JsonKit 走注册表：按模板名序列化/反序列化")
    void kitByTemplateName() {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        if (!registry.contains("gson")) {
            registry.register("gson", new GsonJsonAdapter());
        }

        String json = JsonKit.toJson("gson", new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson("gson", json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("✅ JsonKit.toJson/fromJson 按模板名: " + json);
    }

    @Test
    @DisplayName("JsonKit 即用即弃：直接传适配器（不注册不缓存）")
    void kitDirectAdapter() {
        // 直接 new 一个适配器传进去，用完即弃——注册表完全没参与
        String json = JsonKit.toJson(new GsonJsonAdapter(), new Animal("Tom", 1938));
        Animal back = JsonKit.fromJson(new GsonJsonAdapter(), json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());

        // 注册表里没有"匿名"模板（证明没被缓存）
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        boolean leaked = registry.contains("anonymous");
        System.out.println("✅ 即用即弃: " + json + "（匿名适配器未进入注册表: " + !leaked + "）");
        assertTrue(!leaked);
    }

    @Test
    @DisplayName("JsonKit 工厂版：定制适配器每次 new 即弃（对齐 YshJson 定制思想）")
    void kitFactory() {
        // 工厂版：传 Supplier，每次调用 new 一个定制适配器，用完即弃
        // 这正是 YshJson 定制路径"每次 new Gson、用完即弃"的复刻
        String json = JsonKit.toJson(
                () -> new GsonJsonAdapter(builder -> builder.setDateFormat("yyyy-MM-dd HH:mm:ss")),
                new Animal("Tom", 1938));
        assertTrue(json.contains("\"name\":\"Tom\""));

        // 反序列化同理
        Animal back = JsonKit.fromJson(
                () -> new GsonJsonAdapter(),
                json, Animal.class);
        assertEquals("Tom", back.getName());
        System.out.println("✅ JsonKit 工厂版（即用即弃 + 定制）: " + json);
    }
}
