package top.cskit.json;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolver for {@link JsonField}: scans a class's fields with reflection and
 * builds field-mapping rules.
 *
 * <p>This is the core of the annotation translation layer. It depends only on
 * JDK reflection, never on any JSON library; each adapter translates the
 * resulting rules into its own mechanism.
 */
public final class JsonFieldResolver {

    private JsonFieldResolver() {
    }

    /** Field-mapping rule: Java field name -> JSON field name. */
    public static final class FieldRule {
        /** Java field name. */
        public final String javaName;
        /** JSON field name (falls back to javaName if not annotated). */
        public final String jsonName;
        /** Whether the field is serialized. */
        public final boolean serialize;
        /** Whether the field is deserialized. */
        public final boolean deserialize;

        FieldRule(Field field, JsonField annotation) {
            this.javaName = field.getName();
            // Priority: value() shorthand > name() explicit > Java field name
            String jsonName = (annotation != null && !annotation.value().isEmpty())
                    ? annotation.value()
                    : (annotation != null && !annotation.name().isEmpty())
                    ? annotation.name()
                    : field.getName();
            this.jsonName = jsonName;
            this.serialize = annotation == null || annotation.serialize();
            this.deserialize = annotation == null || annotation.deserialize();
        }
    }

    /**
     * Resolves mapping rules for all fields, including inherited ones.
     *
     * @param clazz target type
     * @return ordered map of javaName -> rule
     */
    public static Map<String, FieldRule> resolve(Class<?> clazz) {
        Map<String, FieldRule> rules = new HashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                // Skip static / transient / synthetic fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isTransient(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }
                JsonField ann = field.getAnnotation(JsonField.class);
                rules.put(field.getName(), new FieldRule(field, ann));
            }
        }
        return rules;
    }
}
