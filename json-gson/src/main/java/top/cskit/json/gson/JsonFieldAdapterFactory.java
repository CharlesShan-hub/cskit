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
 * Annotation translation layer for Gson: intercepts classes carrying
 * {@code @JsonField} and generates a custom {@link TypeAdapter}, so the
 * framework's unified field-mapping annotation works on the Gson backend.
 *
 * <p>How it works:
 * <ol>
 *   <li>On write, translates Java field names to the JSON names from
 *       {@code @JsonField.name()}; fields with {@code serialize=false} are omitted.</li>
 *   <li>On read, reads JSON values by annotation name and writes them back to
 *       Java fields; fields with {@code deserialize=false} are ignored.</li>
 *   <li>Fields without the annotation fall through to Gson's default reflection.</li>
 * </ol>
 */
public class JsonFieldAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        Map<String, JsonFieldResolver.FieldRule> rules = JsonFieldResolver.resolve(rawType);

        // No annotated difference (all rules use defaults) -> do not intercept
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
                        continue; // serialization disabled: skip
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
                        in.skipValue(); // unknown field or deserialization disabled: skip
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
