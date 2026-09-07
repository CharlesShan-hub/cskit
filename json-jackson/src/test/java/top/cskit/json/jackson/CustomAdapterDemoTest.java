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
import top.cskit.json.testkit.Animal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Custom-adapter integration demo: how an in-house JSON library plugs into
 * cskit.
 *
 * <p>Scenario: a company has its own JsonUtil (Gson-based, with a uniform
 * date format) and all business code already uses it. To bring it under the
 * cskit facade, only two steps are needed:
 * <ol>
 *   <li>implement {@link JsonAdapter} (translate the library's API into the 5 facade methods)</li>
 *   <li>register it in {@link JsonAdapterRegistry} (one line)</li>
 * </ol>
 * Afterwards it switches by template name like gson / fastjson / jackson,
 * with zero code change.
 */
class CustomAdapterDemoTest {

    // ==================== Simulated in-house JSON library (assume existing, unmodifiable) ====================

    /**
     * In-house library: Gson-based wrapper with a uniform date format.
     * In reality this is legacy code or a third-party jar we cannot change;
     * we can only adapt to it.
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

    // ==================== Adapter: translate the in-house library into the cskit facade ====================

    /** Company adapter: 5 methods mapped 1:1; business side is unaware. */
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

    // ==================== Verification ====================

    @Test
    @DisplayName("Custom adapter integration: one register line, co-exists with official adapters")
    void customAdapterIntegration() {
        // 0. Ensure official adapters are registered (idempotent; in real apps, done at startup)
        JsonAdapterRegistry reg = JsonAdapterRegistry.getInstance();
        if (!reg.contains("gson")) {
            reg.register("gson", new GsonJsonAdapter());
        }
        if (!reg.contains("fastjson")) {
            reg.register("fastjson", new FastJsonJsonAdapter());
        }

        // 1. Register the custom adapter (this is the whole integration!)
        reg.register("company", new CompanyJsonAdapter());

        // 2. Fetch by template name, exactly like gson/fastjson
        JsonAdapter company = JsonAdapterRegistry.getInstance().get("company");
        JsonAdapter gson = JsonAdapterRegistry.getInstance().get("gson");

        // 3. Serialize: the company library's date convention applies automatically
        Animal tom = new Animal("Tom", 1938);
        String json = company.toJson(tom);
        System.out.println("[company] toJson: " + json);
        assertTrue(json.contains("\"name\":\"Tom\""));

        // 4. Deserialize: same result across adapters
        Animal back = company.fromJson(json, Animal.class);
        assertEquals("Tom", back.getName());
        assertEquals(1938, back.getBirthYear());

        // 5. Generic List supported too
        String listJson = "[{\"name\":\"Tom\",\"birthYear\":1938},{\"name\":\"Jerry\",\"birthYear\":1940}]";
        List<Animal> list = company.fromJsonList(listJson, Animal.class);
        assertEquals("Jerry", list.get(1).getName());

        // 6. Consistent with the official adapter (facade is unified)
        Animal viaGson = gson.fromJson(company.toJson(tom), Animal.class);
        assertEquals(tom.getName(), viaGson.getName());
        assertEquals(tom.getBirthYear(), viaGson.getBirthYear());

        System.out.println("custom adapter integrated: company / gson / fastjson / jackson coexist");
        for (String name : new String[]{"gson", "fastjson", "company"}) {
            JsonAdapter a = JsonAdapterRegistry.getInstance().get(name);
            System.out.println("[" + name + "] " + a.fromJson(json, Animal.class).getName());
        }
    }
}
