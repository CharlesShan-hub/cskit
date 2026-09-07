package top.cskit.json.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 宝宝的想法落地版：命名 Gson 定制注册表
 * <p>
 * 思路：把「定制配置」按名字存进缓存，之后按名字取用，
 * 解决每次传 operation 都要 new Gson 的问题（YshJson 定制路径的痛点）。
 */
class NamedGsonRegistryTest {

    /** 命名注册表：name -> Gson 实例（ConcurrentHashMap 保证多线程安全） */
    static class GsonRegistry {

        private final Map<String, Gson> registry = new ConcurrentHashMap<>();

        /** 注册一个命名定制，返回 this 支持链式 set().set()（正是宝宝说的 set 风格） */
        GsonRegistry set(String name, GsonBuilderCustomizer customizer) {
            GsonBuilder builder = new GsonBuilder();
            customizer.customize(builder);
            registry.put(name, builder.create());
            return this;
        }

        /** 取命名实例（未注册抛异常，防止手滑写错名字） */
        Gson get(String name) {
            Gson gson = registry.get(name);
            if (gson == null) {
                throw new IllegalArgumentException("未注册的 Gson 配置: " + name);
            }
            return gson;
        }

        // ---- 便捷包装：按名字序列化 / 反序列化 ----

        String toJson(String name, Object obj) {
            return get(name).toJson(obj);
        }

        <T> T fromJson(String name, String json, Class<T> cls) {
            return get(name).fromJson(json, cls);
        }

        <T> T fromJson(String name, String json, Type type) {
            return get(name).fromJson(json, type);
        }
    }

    /** 函数式接口：定制 GsonBuilder（等价于 YshJson.GsonInitOperation） */
    @FunctionalInterface
    interface GsonBuilderCustomizer {
        void customize(GsonBuilder builder);
    }

    // region demo 实体

    static class Animal {
        private String name;
        private int birthYear;

        Animal() {
        }

        Animal(String name, int birthYear) {
            this.name = name;
            this.birthYear = birthYear;
        }

        String getName() {
            return name;
        }

        int getBirthYear() {
            return birthYear;
        }
    }

    static class Tourist {
        private String name;
        private Date birthday;

        Tourist() {
        }

        Tourist(String name, Date birthday) {
            this.name = name;
            this.birthday = birthday;
        }
    }

    // endregion

    @Test
    @DisplayName("命名注册表：set().set() 链式注册 + 按名字复用")
    void namedRegistry() {
        GsonRegistry reg = new GsonRegistry()
                .set("pretty-date", b -> b.setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting())
                .set("null-keep", b -> b.serializeNulls())
                .set("default", b -> {
                });

        // ① 用 "pretty-date"：日期格式化 + 缩进
        String pretty = reg.toJson("pretty-date", new Tourist("Elsa", new Date(1710000000000L)));
        System.out.println("[pretty-date]\n" + pretty);
        assertTrue(pretty.contains("2024-03-10"));
        assertTrue(pretty.contains("\n")); // pretty printing 有换行

        // ② 用 "null-keep"：null 字段保留
        String keepNull = reg.toJson("null-keep", new Animal(null, 1938));
        System.out.println("[null-keep] " + keepNull);
        assertTrue(keepNull.contains("\"name\":null"));

        // ③ 用 "default"：默认行为，null 不输出
        String defaultJson = reg.toJson("default", new Animal(null, 1938));
        System.out.println("[default] " + defaultJson);
        assertTrue(!defaultJson.contains("name"));

        // ④ 同一个名字反复用，拿到的都是同一个缓存实例（不会每次 new）
        Gson first = reg.get("pretty-date");
        Gson second = reg.get("pretty-date");
        System.out.println("同一名字是否同一实例: " + (first == second));
        assertEquals(first, second); // 引用相等 = 缓存生效

        // ⑤ 泛型也支持：按名字反序列化 List
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938}]";
        List<Animal> animals = reg.fromJson("default", listJson, new TypeToken<List<Animal>>() {
        }.getType());
        System.out.println("[default] 泛型反序列化: " + animals.get(0).getName());
        assertEquals("Tom", animals.get(0).getName());
    }

    @Test
    @DisplayName("未注册的名字会报错，防手滑")
    void unknownNameThrows() {
        GsonRegistry reg = new GsonRegistry().set("pretty-date", b -> {
        });
        try {
            reg.get("typo-name");
            throw new AssertionError("应该抛异常才对！");
        } catch (IllegalArgumentException e) {
            System.out.println("捕获到预期异常: " + e.getMessage());
            assertTrue(e.getMessage().contains("未注册"));
        }
    }
}
