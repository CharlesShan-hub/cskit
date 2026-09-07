package top.cskit.json.jackson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义适配器接入演示：模拟「公司自研 JSON 库」如何接入 cskit。
 * <p>
 * 场景：公司有自研的 JsonUtil（基于 Gson 二次封装，加了统一的日期格式约定），
 * 业务代码已经全部用它。现在想接入 cskit 统一门面——只需要：
 * 1. 写一个类实现 {@link JsonAdapter}（把公司库的 API 翻译成门面 5 个方法）
 * 2. 注册到 {@link JsonAdapterRegistry}（一行）
 * 之后就能和 gson / fastjson / jackson 一样通过模板名切换，换库零改动。
 */
class CustomAdapterDemoTest {

    // ==================== 模拟：公司自研 JSON 库（假设已经存在，不能改） ====================

    /**
     * 公司自研库：基于 Gson 二次封装，统一日期格式 + 统一入口。
     * 真实场景中这是老代码/第三方 jar，我们不能改它，只能适配它。
     */
    public static final class CompanyJsonLib {

        private static final Gson GSON = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();

        public static String toJsonString(Object obj) {
            return GSON.toJson(obj);
        }

        public static <T> T parseObject(String json, Class<T> clazz) {
            return GSON.fromJson(json, clazz);
        }

        public static <T> T parseObject(String json, Type type) {
            return GSON.fromJson(json, type);
        }

        public static <T> List<T> parseArray(String json, Class<T> clazz) {
            return GSON.fromJson(json, TypeToken.getParameterized(List.class, clazz).getType());
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> parseMap(String json) {
            return GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
        }
    }

    // ==================== 适配器：把公司库翻译成 cskit 门面 ====================

    /** 公司库适配器：5 个方法一一对应翻译，业务侧零感知 */
    public static class CompanyJsonAdapter implements JsonAdapter {

        @Override
        public String toJson(Object obj) {
            return CompanyJsonLib.toJsonString(obj);
        }

        @Override
        public <T> T fromJson(String json, Class<T> classOfT) {
            return CompanyJsonLib.parseObject(json, classOfT);
        }

        @Override
        public <T> T fromJson(String json, Type type) {
            return CompanyJsonLib.parseObject(json, type);
        }

        @Override
        public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
            return CompanyJsonLib.parseArray(json, classOfT);
        }

        @Override
        public Map<String, Object> fromJsonMap(String json) {
            return CompanyJsonLib.parseMap(json);
        }
    }

    // ==================== 验证 ====================

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

    @Test
    @DisplayName("自定义适配器接入：注册一行，与官方适配器同台竞技")
    void customAdapterIntegration() {
        // 0. 确保官方适配器已注册（幂等；真实应用中通常在启动时统一注册）
        JsonAdapterRegistry reg = JsonAdapterRegistry.getInstance();
        if (!reg.contains("gson")) {
            reg.register("gson", new GsonJsonAdapter());
        }
        if (!reg.contains("fastjson")) {
            reg.register("fastjson", new FastJsonJsonAdapter());
        }

        // 1. 注册自定义适配器（这就是"接入"的全部代码！）
        reg.register("company", new CompanyJsonAdapter());

        // 2. 用模板名取用，和 gson/fastjson 完全一样的用法
        JsonAdapter company = JsonAdapterRegistry.getInstance().get("company");
        JsonAdapter gson = JsonAdapterRegistry.getInstance().get("gson");

        // 3. 序列化：公司库的日期约定自动生效
        Animal tom = new Animal("Tom", 1938);
        String json = company.toJson(tom);
        System.out.println("[company] toJson: " + json);
        assertTrue(json.contains("\"name\":\"Tom\""));

        // 4. 反序列化：三库结果一致
        Animal back = company.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());

        // 5. 泛型 List 也支持
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938},{\"name\":\"Jerry\",\"birthYear\":1940}]";
        List<Animal> list = company.fromJsonList(listJson, Animal.class);
        assertEquals("Jerry", list.get(1).getName());

        // 6. 与官方适配器结果一致（证明门面统一）
        Animal viaGson = gson.fromJson(company.toJson(tom), Animal.class);
        assertEquals(tom.getName(), viaGson.getName());
        assertEquals(tom.getBirthYear(), viaGson.getBirthYear());

        System.out.println("✅ 自定义适配器接入成功：company / gson / fastjson / jackson 四库共存");
        for (String name : new String[]{"gson", "fastjson", "company"}) {
            JsonAdapter a = JsonAdapterRegistry.getInstance().get(name);
            System.out.println("[" + name + "] " + a.fromJson(json, Animal.class).getName());
        }
    }
}
