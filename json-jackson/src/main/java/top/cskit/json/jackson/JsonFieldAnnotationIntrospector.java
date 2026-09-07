package top.cskit.json.jackson;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import top.cskit.json.JsonField;

import java.lang.reflect.Field;

/**
 * 框架注解翻译层（Jackson 实现）：
 * 通过扩展 {@link JacksonAnnotationIntrospector}，让框架统一注解 {@code @JsonField}
 * 在 Jackson 底层生效——字段重命名、序列化/反序列化开关。
 * <p>
 * 与 Gson 版（手写 TypeAdapter）不同，Jackson 原生支持注解内省扩展，实现更轻量。
 *
 * <pre>
 * {@literal @}JsonField("ename")            → 序列化/反序列化都用 ename 作为 JSON 字段名
 * {@literal @}JsonField(serialize=false)    → 序列化时忽略（serializeMapper 生效）
 * {@literal @}JsonField(deserialize=false)  → 反序列化时忽略（deserializeMapper 生效）
 * </pre>
 */
public class JsonFieldAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final boolean forSerialization;

    /**
     * @param forSerialization true = 用于序列化方向（忽略 serialize=false）；
     *                         false = 用于反序列化方向（忽略 deserialize=false）
     */
    public JsonFieldAnnotationIntrospector(boolean forSerialization) {
        this.forSerialization = forSerialization;
    }

    /** 序列化时取字段的 JSON 名（@JsonField 优先，否则默认） */
    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        JsonField ann = findAnnotation(a);
        if (ann != null) {
            String jsonName = effectiveName(ann);
            if (!jsonName.isEmpty()) {
                return PropertyName.construct(jsonName);
            }
        }
        return super.findNameForSerialization(a);
    }

    /** 反序列化时取字段的 JSON 名（@JsonField 优先，否则默认） */
    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        JsonField ann = findAnnotation(a);
        if (ann != null) {
            String jsonName = effectiveName(ann);
            if (!jsonName.isEmpty()) {
                return PropertyName.construct(jsonName);
            }
        }
        return super.findNameForDeserialization(a);
    }

    /** 按方向忽略：序列化看 serialize，反序列化看 deserialize */
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        JsonField ann = findAnnotation(m);
        if (ann != null) {
            if (forSerialization && !ann.serialize()) {
                return true;
            }
            if (!forSerialization && !ann.deserialize()) {
                return true;
            }
        }
        return super.hasIgnoreMarker(m);
    }

    // ---- 辅助 ----

    private JsonField findAnnotation(Annotated annotated) {
        if (annotated instanceof AnnotatedMember) {
            Object member = ((AnnotatedMember) annotated).getMember();
            if (member instanceof Field) {
                return ((Field) member).getAnnotation(JsonField.class);
            }
        }
        return annotated.getAnnotation(JsonField.class);
    }

    /** 取值优先级：value() 简写 > name() 显式 > 空（用默认名） */
    private String effectiveName(JsonField ann) {
        if (!ann.value().isEmpty()) {
            return ann.value();
        }
        return ann.name();
    }
}
