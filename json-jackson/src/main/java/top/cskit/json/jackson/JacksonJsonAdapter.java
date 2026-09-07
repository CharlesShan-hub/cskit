package top.cskit.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import top.cskit.json.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Jackson adapter backed by jackson-databind.
 *
 * <p>Integrates {@link JsonFieldAnnotationIntrospector} so the framework's
 * {@code @JsonField} annotation takes effect:
 * <ul>
 *   <li>serializeMapper handles serialization (skips serialize=false, applies rename)</li>
 *   <li>deserializeMapper handles deserialization (skips deserialize=false, applies rename)</li>
 * </ul>
 * ObjectMapper is thread-safe once configured, so the adapter is a reusable singleton.
 */
public class JacksonJsonAdapter implements JsonAdapter {

    /** Serialization mapper: honors @JsonField serialization rules. */
    private final ObjectMapper serializeMapper;
    /** Deserialization mapper: honors @JsonField deserialization rules. */
    private final ObjectMapper deserializeMapper;

    public JacksonJsonAdapter() {
        this.serializeMapper = new ObjectMapper()
                .setAnnotationIntrospector(new JsonFieldAnnotationIntrospector(true));
        this.deserializeMapper = new ObjectMapper()
                .setAnnotationIntrospector(new JsonFieldAnnotationIntrospector(false));
    }

    @Override
    public String toJson(Object obj) {
        try {
            return serializeMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson serialization failed", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return deserializeMapper.readValue(json, classOfT);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson deserialization failed", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        try {
            return deserializeMapper.readValue(json, deserializeMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new IllegalStateException("Jackson deserialization failed", e);
        }
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        try {
            CollectionType type = deserializeMapper.getTypeFactory()
                    .constructCollectionType(List.class, classOfT);
            return deserializeMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson deserialization failed for List", e);
        }
    }

    @Override
    public Map<String, Object> fromJsonMap(String json) {
        try {
            MapType type = deserializeMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class);
            return deserializeMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson deserialization failed for Map", e);
        }
    }
}
