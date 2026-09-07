# cskit-json — JSON 统一门面框架

> 一套 API，任意底层库，可插拔适配器。
> 业务代码只面向 `top.cskit.json.JsonAdapter` 接口，底层想用谁，在注册表里选一个模板名，**换库零改动**。

## 为什么要有这个框架？

Java 世界有 Gson、Fastjson、Jackson 等众多 JSON 库，各有优缺点：

| 库 | 优点 | 痛点 |
|---|---|---|
| Gson | 反射读字段、轻量、稳定 | 性能一般、注解能力弱 |
| Fastjson2 | 性能极强（实测快 3~6 倍） | 字段可见性要求高、1.x 不兼容 JDK17+ |
| Jackson | 功能全 | 配置繁琐、注解侵入 |

**问题**：业务代码一旦直接依赖某个库，想换库就要改所有调用处，成本极高。

**方案**：把「门面接口 + 底层适配」分离——业务代码只面向 `JsonAdapter` 接口，
底层想用谁，**在注册表里选一个模板名**，换库零改动。

## 架构

```
┌─────────────────────────────────────────────────────┐
│                    业务代码                          │
│          只依赖 top.cskit.json.JsonAdapter           │
└─────────────────────────┬───────────────────────────┘
                          │ 使用
┌─────────────────────────▼───────────────────────────┐
│            JsonAdapterRegistry（注册表）              │
│        register("gson", ...) / register("fastjson")  │
└──────┬──────────────────────────┬───────────────────┘
       │                          │
┌──────▼──────┐          ┌────────▼─────────┐
│ json-gson   │          │ json-fastjson    │
│ (Gson 适配) │          │ (Fastjson2 适配) │
└──────┬──────┘          └────────┬─────────┘
       │                          │
┌──────▼──────────────────────────▼─────────┐
│            json-core（零第三方依赖）        │
│   JsonAdapter 接口 + 注册表 + @JsonField   │
└───────────────────────────────────────────┘
```

## 模块说明

| 模块 | 职责 | 第三方依赖 |
|---|---|---|
| `json-core` | 门面接口、注册表、`@JsonField` 注解、字段解析器 | **零依赖** |
| `json-gson` | Gson 适配器 + 注解翻译层 | gson |
| `json-fastjson` | Fastjson2 适配器 | fastjson2 |

> **依赖隔离**：只想用 Gson 的项目只引 `json-core` + `json-gson`，fastjson2 不会进入 classpath。

## 快速开始

### 1. 引入依赖

```xml
<!-- 核心 + 想要的适配器（以 Gson 为例） -->
<dependency>
    <groupId>top.cskit</groupId>
    <artifactId>json-core</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>top.cskit</groupId>
    <artifactId>json-gson</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. 注册 + 使用

```java
// 注册表：预注册需要的适配器（可运行时扩展）
JsonAdapterRegistry registry = new JsonAdapterRegistry()
        .register("gson", new GsonJsonAdapter())
        .register("fastjson", new FastJsonJsonAdapter());

// 模板选择：换库只改这一行
JsonAdapter adapter = registry.get("gson");

// 业务代码只面向接口
String json = adapter.toJson(user);                          // 序列化
User u = adapter.fromJson(json, User.class);                 // 反序列化
List<User> list = adapter.fromJsonList(json, User.class);    // 泛型 List
Map<String, Object> map = adapter.fromJsonMap(json);         // Map
```

### 3. 统一注解 `@JsonField`

一套注解，各适配器统一生效（Gson 已支持，其他适配器按需扩展）：

```java
public class Employee {
    @JsonField("ename")                // 字段重命名（JSON 用 ename）
    private String name;

    @JsonField(serialize = false)      // 只读不写（对外隐藏）
    private double cost;

    @JsonField(deserialize = false)    // 只写不读（拒绝外部注入）
    private double profit;
}
```

### 4. 新增底层库适配（如 Jackson）

```java
public class JacksonJsonAdapter implements JsonAdapter {
    // 实现 5 个接口方法即可
}
// 然后注册：
registry.register("jackson", new JacksonJsonAdapter());
```

## 性能评测（实测）

数据集 1000 条记录，预热 2000 次 + 测量 20000 次（本机 JDK 21）：

| 实现 | 序列化 | 反序列化 | JSON 体积 |
|---|---|---|---|
| GsonJsonAdapter | ~120 μs | ~130 μs | 38,891 bytes |
| FastJsonJsonAdapter | ~25 μs | ~35 μs | 38,891 bytes |

> fastjson2 序列化快约 **4~6 倍**。评测代码见 `json-gson` 模块的 `JsonAdapterBenchmarkTest`。

## 设计亮点

1. **适配器模式**：门面接口与底层实现解耦，换库零改动（对标 JDBC / slf4j）
2. **注册表模式**：命名模板 + fail-fast 防手滑（未注册即抛异常）
3. **依赖隔离**：core 零第三方依赖，各适配器独立模块按需引入
4. **统一注解**：`@JsonField` 一套注解翻译到各底层库，业务代码不感知底层差异
5. **线程安全**：Gson 实例线程安全，适配器单例复用；注册表用 `ConcurrentHashMap`

## 构建测试

```bash
mvn test
```

## License

MIT
