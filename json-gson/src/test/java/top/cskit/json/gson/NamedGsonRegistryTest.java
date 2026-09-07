package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.testkit.Animal;
import top.cskit.json.testkit.Tourist;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 命名定制注册表（吃自己的狗粮版）
 * <p>
 * 验证「命名注册表」设计：把定制配置按名字注册进 {@link JsonAdapterRegistry}，
 * 之后按名字取用——这正是宝宝最早提出的思路，如今由框架自身的
 * {@link JsonAdapterRegistry} + 定制构造的 {@link GsonJsonAdapter} 实现。
 * 实体复用 {@link Animal} / {@link Tourist}（共享测试实体）。
 */
class NamedGsonRegistryTest {

    /** 注册表（全局单例）：注册几个命名定制的 Gson 适配器 */
    static final JsonAdapterRegistry REGISTRY = JsonAdapterRegistry.getInstance()
            .register("pretty-date", new GsonJsonAdapter(builder ->
                    builder.setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting()))
            .register("null-keep", new GsonJsonAdapter(builder -> builder.serializeNulls()))
            .register("default", new GsonJsonAdapter());

    @Test
    @DisplayName("命名定制：set().set() 链式注册 + 按名字复用")
    void namedRegistry() {
        // ① 用 "pretty-date"：日期格式化 + 缩进
        String pretty = REGISTRY.get("pretty-date").toJson(new Tourist("Elsa", new Date(1710000000000L)));
        System.out.println("[pretty-date]\n" + pretty);
        assertTrue(pretty.contains("2024-03-10"));
        assertTrue(pretty.contains("\n")); // pretty printing 有换行

        // ② 用 "null-keep"：null 字段保留
        String keepNull = REGISTRY.get("null-keep").toJson(new Animal(null, 1938));
        System.out.println("[null-keep] " + keepNull);
        assertTrue(keepNull.contains("\"name\":null"));

        // ③ 用 "default"：默认行为，null 不输出
        String defaultJson = REGISTRY.get("default").toJson(new Animal(null, 1938));
        System.out.println("[default] " + defaultJson);
        assertTrue(!defaultJson.contains("name"));

        // ④ 同一个名字反复取用，拿到的是同一个注册实例（缓存生效）
        JsonAdapter first = REGISTRY.get("pretty-date");
        JsonAdapter second = REGISTRY.get("pretty-date");
        System.out.println("同一名字是否同一实例: " + (first == second));
        assertSame(first, second); // 引用相等 = 缓存生效

        // ⑤ 泛型也支持：按名字反序列化 List
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938}]";
        List<Animal> animals = REGISTRY.get("default").fromJsonList(listJson, Animal.class);
        System.out.println("[default] 泛型反序列化: " + animals.get(0).getName());
        assertEquals("Tom", animals.get(0).getName());
    }

    @Test
    @DisplayName("未注册的名字会报错，防手滑（fail-fast）")
    void unknownNameThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> REGISTRY.get("typo-name"));
        System.out.println("捕获到预期异常: " + e.getMessage());
        assertTrue(e.getMessage().contains("未注册"));
    }
}
