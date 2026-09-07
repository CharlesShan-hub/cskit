package top.cskit.json.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import top.cskit.json.JsonFieldResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * 框架注解翻译层（Gson 实现）：
 * 拦截标注了 {@code @JsonField} 的类，生成自定义 {@link TypeAdapter}，
 * 使框架统一的字段映射注解在 Gson 底层生效。
 * <p>
 * 工作原理：
 * 1. 序列化时，把 Java 字段名翻译成 {@code @JsonField.name()} 指定的 JSON 名；
 *    被 {@code @JsonField(serialize=false)} 的字段不输出。
 * 2. 反序列化时，从 JSON 中按注解名读取并写回 Java 字段；
 *    被 {@code @JsonField(deserialize=false)} 的字段不接收。
 * 3. 未标注注解的字段走 Gson 默认反射逻辑（由工厂委托下游处理）。
 */
public class JsonFieldAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        Map<String, JsonFieldResolver.FieldRule> rules = JsonFieldResolver.resolve(rawType);

        // 没有任何字段有注解差异（有规则但 name/serialize/deserialize 全默认）→ 不拦截
        boolean hasAnnotation = rules.values().stream()
                .anyMatch(r -> r.serialize == false
                        || r.deserialize == false
                        || !r.jsonName.equals(r.javaName));
        if (!hasAnnotation) {
            return null;
        }

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                out.beginObject();
                for (Map.Entry<String, JsonFieldResolver.FieldRule> entry : rules.entrySet()) {
                    JsonFieldResolver.FieldRule rule = entry.getValue();
                    if (!rule.serialize) {
                        continue; // 序列化关闭：跳过
                    }
                    Field field = getField(rawType, rule.javaName);
                    Object fieldValue = readField(field, value);
                    out.name(rule.jsonName);
                    gson.toJson(fieldValue, field.getGenericType(), out);
                }
                out.endObject();
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                T instance = newInstance(rawType);
                in.beginObject();
                while (in.hasNext()) {
                    String jsonName = in.nextName();
                    JsonFieldResolver.FieldRule rule = findRuleByJsonName(jsonName);
                    if (rule == null || !rule.deserialize) {
                        in.skipValue(); // 未知字段或反序列化关闭：跳过
                        continue;
                    }
                    Field field = getField(rawType, rule.javaName);
                    Object value = gson.fromJson(in, field.getGenericType());
                    writeField(field, instance, value);
                }
                in.endObject();
                return instance;
            }

            private JsonFieldResolver.FieldRule findRuleByJsonName(String jsonName) {
                for (JsonFieldResolver.FieldRule rule : rules.values()) {
                    if (rule.jsonName.equals(jsonName)) {
                        return rule;
                    }
                }
                return null;
            }
        };
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
