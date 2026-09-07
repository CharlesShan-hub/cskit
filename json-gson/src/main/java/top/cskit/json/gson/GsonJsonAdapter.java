package top.cskit.json.gson;

import top.cskit.json.JsonAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Gson 适配器：内部直接使用 Gson（不走 YshJson，展示适配器如何封装任意底层库）。
 * <p>
 * 复用 Gson 线程安全特性，单例持有，避免重复构建。
 * 已注册 {@link JsonFieldAdapterFactory}，使框架统一注解 {@code @JsonField} 在 Gson 底层生效。
 * <p>
 * 支持定制构造（对齐 YshJson 的 GsonInitOperation 设计）：
 * <pre>
 * // 定制日期格式 + 保留 null 的适配器
 * JsonAdapter adapter = new GsonJsonAdapter(builder -&gt;
 *         builder.setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls());
 * </pre>
 */
public class GsonJsonAdapter implements JsonAdapter {

    private final Gson gson;

    /** 默认构造：仅注册框架注解翻译层 */
    public GsonJsonAdapter() {
        this(builder -> {
        });
    }

    /**
     * 定制构造：在框架注解翻译层基础上，追加 GsonBuilder 定制
     *
     * @param customizer 定制函数（可配置日期格式、null 策略等）
     */
    public GsonJsonAdapter(Consumer<GsonBuilder> customizer) {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapterFactory(new JsonFieldAdapterFactory());
        customizer.accept(builder);
        this.gson = builder.create();
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
