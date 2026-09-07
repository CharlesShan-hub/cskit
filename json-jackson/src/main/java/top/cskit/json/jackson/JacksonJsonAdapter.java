package top.cskit.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import top.cskit.json.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Jackson 适配器：内部使用 jackson-databind。
 * <p>
 * 已接入 {@link JsonFieldAnnotationIntrospector}，使框架统一注解 {@code @JsonField} 生效：
 * <ul>
 *   <li>序列化使用 serializeMapper（忽略 serialize=false 字段，应用字段重命名）</li>
 *   <li>反序列化使用 deserializeMapper（忽略 deserialize=false 字段，应用字段重命名）</li>
 * </ul>
 * ObjectMapper 配置完成后线程安全，适配器可单例复用。
 */
public class JacksonJsonAdapter implements JsonAdapter {

    /** 序列化专用 mapper：识别 @JsonField 的序列化规则 */
    private final ObjectMapper serializeMapper;
    /** 反序列化专用 mapper：识别 @JsonField 的反序列化规则 */
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
            throw new IllegalStateException("Jackson 序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return deserializeMapper.readValue(json, classOfT);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        try {
            return deserializeMapper.readValue(json, deserializeMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new IllegalStateException("Jackson 反序列化失败", e);
        }
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        try {
            CollectionType type = deserializeMapper.getTypeFactory()
                    .constructCollectionType(List.class, classOfT);
            return deserializeMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson 反序列化 List 失败", e);
        }
    }

    @Override
    public Map<String, Object> fromJsonMap(String json) {
        try {
            MapType type = deserializeMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class);
            return deserializeMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Jackson 反序列化 Map 失败", e);
        }
    }
}
