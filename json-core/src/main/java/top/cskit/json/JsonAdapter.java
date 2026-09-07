package top.cskit.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Unified JSON adapter interface (the framework facade).
 *
 * <p>Business code depends only on this interface. Underlying implementations
 * (Gson, Fastjson, Jackson...) can be swapped freely via
 * {@link JsonAdapterRegistry} by name, with zero code change.
 */
public interface JsonAdapter {

    /** Serializes an object to a JSON string. */
    String toJson(Object obj);

    /** Deserializes JSON to an object of the given type (non-generic). */
    <T> T fromJson(String json, Class<T> classOfT);

    /** Deserializes JSON to an object of a generic type, e.g. {@code List<T>}, {@code Map<String,T>}. */
    <T> T fromJson(String json, Type type);

    /** Deserializes JSON to a list of the given element type. */
    <T> List<T> fromJsonList(String json, Class<T> classOfT);

    /** Deserializes JSON to a map. */
    Map<String, Object> fromJsonMap(String json);
}
