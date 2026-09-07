package top.cskit.json;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link JsonField} 注解解析器：反射扫描类的字段，生成字段映射规则。
 * <p>
 * 这是框架注解翻译层的核心：只依赖 JDK 反射，不绑定任何底层 JSON 库，
 * 各适配器（Gson / Fastjson）拿到规则后各自翻译为自己的实现。
 */
public final class JsonFieldResolver {

    private JsonFieldResolver() {
    }

    /** 字段映射规则：Java 字段名 -> JSON 字段名 */
    public static final class FieldRule {
        /** Java 字段名 */
        public final String javaName;
        /** JSON 字段名（未注解或未指定时 = javaName） */
        public final String jsonName;
        /** 是否参与序列化 */
        public final boolean serialize;
        /** 是否参与反序列化 */
        public final boolean deserialize;

        FieldRule(Field field, JsonField annotation) {
            this.javaName = field.getName();
            // 取值优先级：value() 简写 > name() 显式 > Java 字段原名
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
     * 解析类所有字段（含继承字段）的映射规则
     *
     * @param clazz 目标类型
     * @return javaName -> rule 的有序映射
     */
    public static Map<String, FieldRule> resolve(Class<?> clazz) {
        Map<String, FieldRule> rules = new HashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                // 跳过 static / transient / synthetic 字段
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
