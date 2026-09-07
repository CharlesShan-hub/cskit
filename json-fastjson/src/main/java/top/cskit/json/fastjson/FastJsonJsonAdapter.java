package top.cskit.json.fastjson;

import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonFieldResolver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Fastjson adapter backed by Alibaba Fastjson2 (fastjson 1.x is no longer
 * maintained and does not support JDK 17+).
 *
 * <p>Integrates {@link JsonFieldFastJsonCodec}: classes annotated with
 * {@code @JsonField} go through custom streaming codec (rename/switches take
 * effect); unannotated classes use fastjson2's native API (no performance
 * loss).
 *
 * <p>Like the Gson/Jackson adapters, it implements the same {@link JsonAdapter}
 * interface; swapping or upgrading a backend only changes this class, business
 * code stays untouched.
 */
public class FastJsonJsonAdapter implements JsonAdapter {

    @Override
    public String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        if (JsonFieldFastJsonCodec.hasAnnotationDiff(clazz)) {
            return JsonFieldFastJsonCodec.toJson(obj, JsonFieldResolver.resolve(clazz));
        }
        return JSON.toJSONString(obj);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        if (JsonFieldFastJsonCodec.hasAnnotationDiff(classOfT)) {
            return JsonFieldFastJsonCodec.fromJson(json, classOfT, JsonFieldResolver.resolve(classOfT));
        }
        return JSON.parseObject(json, classOfT);
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        return JSON.parseArray(json, classOfT);
    }

    @Override
    public Map<String, Object> fromJsonMap(String json) {
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
        });
    }
}
