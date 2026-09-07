package top.cskit.json.gson;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 宝宝自研 JSON 框架的适配器模式演示
 * <p>
 * 核心思想：业务代码只面向 {@link JsonAdapter} 接口，
 * 底层实现通过注册表 + 模板名切换（gson / fastjson），换库零改动。
 */
class JsonAdapterDemoTest {

    /** 注册表（全局单例）：预注册 gson 和 fastjson 两个模板 */
    static final JsonAdapterRegistry REGISTRY = JsonAdapterRegistry.getInstance()
            .register("gson", new GsonJsonAdapter())
            .register("fastjson", new FastJsonJsonAdapter());

    public static class Animal {
        private String name;
        private int birthYear;

        public Animal() {
        }

        public Animal(String name, int birthYear) {
            this.name = name;
            this.birthYear = birthYear;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(int birthYear) {
            this.birthYear = birthYear;
        }
    }

    // region 核心演示：同一业务代码，两个底层库

    @Test
    @DisplayName("模板切换：同一段业务代码，gson / fastjson 都能跑")
    void templateSwitch() {
        Animal tom = new Animal("Tom", 1938);

        for (String template : new String[]{"gson", "fastjson"}) {
            JsonAdapter adapter = REGISTRY.get(template); // ← 模板选择就在这一行！

            // 序列化
            String json = adapter.toJson(tom);
            System.out.println("[" + template + "] toJson: " + json);
            assertTrue(json.contains("\"name\":\"Tom\""), template + " 序列化结果应有 name");

            // 反序列化
            Animal back = adapter.fromJson(json, Animal.class);
            System.out.println("[" + template + "] fromJson: " + back.getName() + " / " + back.getBirthYear());
            assertEquals("Tom", back.getName());
            assertEquals(1938, back.getBirthYear());
        }
    }

    @Test
    @DisplayName("泛型 & 集合：两个库的 List / Map 都支持")
    void genericCollection() {
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938},{\"name\":\"Jerry\",\"birthYear\":1940}]";
        String mapJson = "{\"cat\":{\"name\":\"Tom\",\"birthYear\":1938}}";

        for (String template : new String[]{"gson", "fastjson"}) {
            JsonAdapter adapter = REGISTRY.get(template);

            List<Animal> list = adapter.fromJsonList(listJson, Animal.class);
            System.out.println("[" + template + "] List[0]: " + list.get(0).getName());
            assertEquals("Tom", list.get(0).getName());

            Map<String, Object> map = adapter.fromJsonMap(mapJson);
            System.out.println("[" + template + "] Map: " + map.keySet());
            assertEquals("Tom", ((Map<?, ?>) map.get("cat")).get("name"));
        }
    }

    // endregion

    // region 框架健壮性

    @Test
    @DisplayName("未注册的模板名会报错（fail-fast）")
    void unknownTemplate() {
        assertThrows(IllegalArgumentException.class, () -> REGISTRY.get("jackson"));
    }

    @Test
    @DisplayName("注册表是全局单例：getInstance 永远返回同一实例")
    void singleton() {
        JsonAdapterRegistry a = JsonAdapterRegistry.getInstance();
        JsonAdapterRegistry b = JsonAdapterRegistry.getInstance();
        System.out.println("两次 getInstance 是否同一实例: " + (a == b));
        assertEquals(a, b, "单例注册表应返回同一实例");

        // 单例的好处：别处注册的模板，这里立即可见
        assertTrue(a.contains("gson"));
        assertTrue(a.contains("fastjson"));
    }

    // endregion
}
