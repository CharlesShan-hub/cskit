package top.cskit.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 框架统一字段映射注解：指定 JSON 字段名与序列化/反序列化开关。
 * <p>
 * 对标 Gson 的 {@code @SerializedName} + {@code @Expose}、Fastjson 的 {@code @JSONField}，
 * 但<b>框架自研一套</b>，通过各适配器的翻译层统一生效——
 * 业务代码只认本注解，换底层库无需改注解。
 *
 * <pre>
 * public class Employee {
 *     {@literal @}JsonField("ename")            // JSON 字段叫 ename
 *     private String name;
 *
 *     {@literal @}JsonField(serialize = false)  // 只读不写（如"老板利润"）
 *     private double cost;
 * }
 * </pre>
 *
 * @see JsonFieldResolver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonField {

    /**
     * 简写：JSON 中的字段名（等价于 {@code name()}）。
     * 用法：{@code @JsonField("ename")}
     */
    String value() default "";

    /**
     * JSON 中的字段名；默认空串表示使用 Java 字段原名。
     * 与 {@code value()} 二选一即可。
     */
    String name() default "";

    /**
     * 是否参与序列化（toJson），默认 true
     */
    boolean serialize() default true;

    /**
     * 是否参与反序列化（fromJson），默认 true
     */
    boolean deserialize() default true;
}
