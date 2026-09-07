package top.cskit.json.fastjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import top.cskit.json.JsonFieldResolver;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 框架注解翻译层（Fastjson 实现）：
 * 通过「字段映射中转」实现——序列化时按 {@code @JsonField} 规则把对象转成
 * 键为注解名的 {@link JSONObject} 再交给 fastjson2；反序列化时把 JSONObject
 * 按规则读回 Java 字段。
 * <p>
 * 与 Gson 版（TypeAdapterFactory）、Jackson 版（AnnotationIntrospector）不同，
 * Fastjson 没有注解内省扩展点，因此采用「中转映射」策略，逻辑直白、不依赖
 * fastjson2 内部流式 API。
 */
public final class JsonFieldFastJsonCodec {

    private JsonFieldFastJsonCodec() {
    }

    /** 类是否含有注解差异（重命名或开关），决定是否需要走自定义编解码 */
    public static boolean hasAnnotationDiff(Class<?> clazz) {
        return JsonFieldResolver.resolve(clazz).values().stream()
                .anyMatch(r -> !r.serialize || !r.deserialize || !r.jsonName.equals(r.javaName));
    }

    /** 序列化：按注解规则转成 JSONObject（键 = 注解名），再交给 fastjson2 */
    public static String toJson(Object obj, Map<String, JsonFieldResolver.FieldRule> rules) {
        JSONObject mapped = new JSONObject(new LinkedHashMap<>());
        for (Map.Entry<String, JsonFieldResolver.FieldRule> entry : rules.entrySet()) {
            JsonFieldResolver.FieldRule rule = entry.getValue();
            if (!rule.serialize) {
                continue; // 序列化关闭：跳过
            }
            Field field = getField(obj.getClass(), rule.javaName);
            mapped.put(rule.jsonName, readField(field, obj));
        }
        return JSON.toJSONString(mapped);
    }

    /** 反序列化：先解析成 JSONObject，再按注解规则读回 Java 字段 */
    public static <T> T fromJson(String json, Class<T> clazz, Map<String, JsonFieldResolver.FieldRule> rules) {
        JSONObject parsed = JSON.parseObject(json);
        T instance = newInstance(clazz);
        for (Map.Entry<String, JsonFieldResolver.FieldRule> entry : rules.entrySet()) {
            JsonFieldResolver.FieldRule rule = entry.getValue();
            if (!rule.deserialize) {
                continue; // 反序列化关闭：跳过
            }
            if (!parsed.containsKey(rule.jsonName)) {
                continue; // JSON 中没有该字段
            }
            Field field = getField(clazz, rule.javaName);
            Object value = parsed.get(rule.jsonName);
            writeField(field, instance, convert(value, field.getType()));
        }
        return instance;
    }

    /**
     * 类型适配：fastjson2 的 JSONObject 数值默认是 BigDecimal/Long，
     * 反射写入原始类型字段前需转成字段声明的类型（Field.set 不做自动拆箱）。
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

    // ---- 反射辅助 ----

    private static Field getField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // 继续向父类查找
            }
        }
        throw new IllegalStateException("字段不存在: " + clazz.getName() + "#" + name);
    }

    private static Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取字段失败: " + field.getName(), e);
        }
    }

    private static void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("写入字段失败: " + field.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("无法创建实例: " + clazz.getName() + "（需要无参构造器）", e);
        }
    }
}
