package top.cskit.json.jackson;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import top.cskit.json.JsonField;

import java.lang.reflect.Field;

/**
 * Annotation translation layer for Jackson: extends
 * {@link JacksonAnnotationIntrospector} so the framework's {@code @JsonField}
 * takes effect on the Jackson backend — field renaming and
 * serialize/deserialize switches.
 *
 * <p>Unlike the Gson version (hand-written TypeAdapter), Jackson natively
 * supports annotation introspection, so this implementation is lighter.
 *
 * <pre>
 * {@literal @}JsonField("ename")            -> both directions use "ename"
 * {@literal @}JsonField(serialize=false)    -> skipped when serializing
 * {@literal @}JsonField(deserialize=false)  -> skipped when deserializing
 * </pre>
 */
public class JsonFieldAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final boolean forSerialization;

    /**
     * @param forSerialization true for the serialization mapper (skip
     *                         serialize=false); false for deserialization
     *                         (skip deserialize=false)
     */
    public JsonFieldAnnotationIntrospector(boolean forSerialization) {
        this.forSerialization = forSerialization;
    }

    /** Returns the JSON name on serialization (@JsonField first, else default). */
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

    /** Returns the JSON name on deserialization (@JsonField first, else default). */
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

    /** Ignores fields by direction: serialize flag for writing, deserialize for reading. */
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

    // ---- Helpers ----

    private JsonField findAnnotation(Annotated annotated) {
        if (annotated instanceof AnnotatedMember) {
            Object member = ((AnnotatedMember) annotated).getMember();
            if (member instanceof Field) {
                return ((Field) member).getAnnotation(JsonField.class);
            }
        }
        return annotated.getAnnotation(JsonField.class);
    }

    /** Priority: value() shorthand > name() explicit > empty (use default). */
    private String effectiveName(JsonField ann) {
        if (!ann.value().isEmpty()) {
            return ann.value();
        }
        return ann.name();
    }
}
