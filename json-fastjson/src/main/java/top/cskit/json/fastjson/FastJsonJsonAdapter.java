package top.cskit.json.fastjson;

import top.cskit.json.JsonAdapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Fastjson 适配器：内部使用阿里 Fastjson2（fastjson 1.x 已停维护，不支持 JDK17+）。
 * <p>
 * 与 Gson 适配器实现同一接口——这就是适配器模式的核心：
 * 底层库升级/替换时，只需改本适配器内部实现，上层业务代码无感知。
 */
public class FastJsonJsonAdapter implements JsonAdapter {

    @Override
    public String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return JSON.parseObject(json, classOfT);
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        return JSON.parseArray(json, classOfT);
    }

    @Override
    public Map<String, Object> fromJsonMap(String json) {
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
        });
    }
}
