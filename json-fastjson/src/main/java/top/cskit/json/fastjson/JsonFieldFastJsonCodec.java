package top.cskit.json.fastjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import top.cskit.json.JsonFieldResolver;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Annotation translation layer for Fastjson: a "map-and-forward" strategy.
 * On write, the object is mapped to a {@link JSONObject} keyed by the
 * annotation names and handed to fastjson2; on read, the JSONObject is
 * mapped back into Java fields.
 *
 * <p>Unlike Gson ({@code TypeAdapterFactory}) and Jackson
 * ({@code AnnotationIntrospector}), Fastjson has no annotation introspection
 * extension point, so this straightforward map-and-forward approach is used,
 * without depending on fastjson2's internal streaming API.
 */
public final class JsonFieldFastJsonCodec {

    private JsonFieldFastJsonCodec() {
    }

    /** Whether the class has annotation differences (rename or switches). */
    public static boolean hasAnnotationDiff(Class<?> clazz) {
        return JsonFieldResolver.resolve(clazz).values().stream()
                .anyMatch(r -> !r.serialize || !r.deserialize || !r.jsonName.equals(r.javaName));
    }

    /** Serializes via an intermediate JSONObject keyed by annotation names. */
    public static String toJson(Object obj, Map<String, JsonFieldResolver.FieldRule> rules) {
        JSONObject mapped = new JSONObject(new LinkedHashMap<>());
        for (Map.Entry<String, JsonFieldResolver.FieldRule> entry : rules.entrySet()) {
            JsonFieldResolver.FieldRule rule = entry.getValue();
            if (!rule.serialize) {
                continue; // serialization disabled: skip
            }
            Field field = getField(obj.getClass(), rule.javaName);
            mapped.put(rule.jsonName, readField(field, obj));
        }
        return JSON.toJSONString(mapped);
    }

    /** Deserializes: parses to JSONObject, then maps values back to fields. */
    public static <T> T fromJson(String json, Class<T> clazz, Map<String, JsonFieldResolver.FieldRule> rules) {
        JSONObject parsed = JSON.parseObject(json);
        T instance = newInstance(clazz);
        for (Map.Entry<String, JsonFieldResolver.FieldRule> entry : rules.entrySet()) {
            JsonFieldResolver.FieldRule rule = entry.getValue();
            if (!rule.deserialize) {
                continue; // deserialization disabled: skip
            }
            if (!parsed.containsKey(rule.jsonName)) {
                continue; // field absent from JSON
            }
            Field field = getField(clazz, rule.javaName);
            Object value = parsed.get(rule.jsonName);
            writeField(field, instance, convert(value, field.getType()));
        }
        return instance;
    }

    /**
     * Type adaptation: fastjson2 JSONObject numbers default to BigDecimal/Long;
     * convert to the declared field type before reflection write (Field.set
     * does no auto-unboxing).
     */
    private static Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return ((Number) value).doubleValue();
        }
        if (targetType == float.class || targetType == Float.class) {
            return ((Number) value).floatValue();
        }
        if (targetType == short.class || targetType == Short.class) {
            return ((Number) value).shortValue();
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return ((Number) value).byteValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return value;
        }
        return value;
    }

    // ---- Reflection helpers ----

    private static Field getField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // continue up the class hierarchy
            }
        }
        throw new IllegalStateException("Field not found: " + clazz.getName() + "#" + name);
    }

    private static Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read field: " + field.getName(), e);
        }
    }

    private static void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to write field: " + field.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate: " + clazz.getName() + " (no-arg constructor required)", e);
        }
    }
}
