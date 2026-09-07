package top.cskit.json.gson;

import top.cskit.json.JsonAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Gson 适配器：内部直接使用 Gson（不走 YshJson，展示适配器如何封装任意底层库）。
 * <p>
 * 复用 Gson 线程安全特性，单例持有，避免重复构建。
 * 已注册 {@link JsonFieldAdapterFactory}，使框架统一注解 {@code @JsonField} 在 Gson 底层生效。
 */
public class GsonJsonAdapter implements JsonAdapter {

    private final Gson gson;

    public GsonJsonAdapter() {
        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JsonFieldAdapterFactory())
                .create();
    }

    @Override
    public String toJson(Object obj) {
        return gson.toJson(obj);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        Type type = TypeToken.getParameterized(List.class, classOfT).getType();
        return gson.fromJson(json, type);
    }

    @Override
    public Map<String, Object> fromJsonMap(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}
