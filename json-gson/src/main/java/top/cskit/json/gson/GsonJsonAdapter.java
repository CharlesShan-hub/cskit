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
 * Gson adapter: wraps Gson directly (shows how any backend is adapted).
 *
 * <p>Gson is thread-safe, so a single instance is kept. Registers
 * {@link JsonFieldAdapterFactory} so the framework's {@code @JsonField}
 * annotation takes effect on the Gson backend.
 *
 * <p>Customizable construction:
 * <pre>
 * JsonAdapter adapter = new GsonJsonAdapter(builder -&gt;
 *         builder.setDateFormat("yyyy-MM-dd HH:mm:ss").serializeNulls());
 * </pre>
 */
public class GsonJsonAdapter implements JsonAdapter {

    private final Gson gson;

    /** Default constructor: only registers the annotation translation layer. */
    public GsonJsonAdapter() {
        this(builder -> {
        });
    }

    /**
     * Custom constructor: applies extra GsonBuilder customization on top of
     * the annotation translation layer.
     *
     * @param customizer e.g. date format, null strategy
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
