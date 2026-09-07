package top.cskit.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Unified field-mapping annotation: sets the JSON field name and the
 * serialize/deserialize switches.
 *
 * <p>One annotation for all backends. Unlike Gson's {@code @SerializedName} /
 * {@code @Expose} or Fastjson's {@code @JSONField}, it is translated by each
 * adapter, so business code never changes when the backend is swapped.
 *
 * <pre>
 * public class Employee {
 *     {@literal @}JsonField("ename")            // JSON field is "ename"
 *     private String name;
 *
 *     {@literal @}JsonField(serialize = false)  // deserialize only
 *     private double cost;
 * }
 * </pre>
 *
 * @see JsonFieldResolver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonField {

    /** Shorthand for the JSON field name (same as {@code name()}). */
    String value() default "";

    /** JSON field name; empty means use the Java field name. */
    String name() default "";

    /** Whether to serialize (toJson). Default true. */
    boolean serialize() default true;

    /** Whether to deserialize (fromJson). Default true. */
    boolean deserialize() default true;
}
