# cskit-json — JSON 统一门面框架

[English](json.md) | [中文](json_zh.md)

> 一套 API，任意底层库，可插拔适配器。
> 业务代码只面向 `top.cskit.json.JsonAdapter` 接口，底层想用谁，在注册表里选一个模板名，**换库零改动**。

## 为什么要有这个框架？

Java 世界有 Gson、Fastjson、Jackson 等众多 JSON 库，各有优缺点：

| 库 | 优点 | 痛点 |
|---|---|---|
| Gson | 反射读字段、轻量、稳定 | 性能一般、注解能力弱 |
| Fastjson2 | 性能极强（实测快 3~14 倍） | 字段可见性要求高、1.x 不兼容 JDK17+ |
| Jackson | 功能全、生态大、性能好 | 配置繁琐、注解侵入 |

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
| `json-jackson` | Jackson 适配器 + 注解翻译层 | jackson-databind |

> **依赖隔离**：只想用 Gson 的项目只引 `json-core` + `json-gson`，其他库不会进入 classpath。

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

### 2. JsonKit —— 统一方法名 + 重载（一个名字，四种用法）

同一个方法名，不同参数类型 = 不同使用模式（避免一堆不同名字的 API）：

```java
// ① 默认适配器（configure 一次即可，对齐 YshJson 的 DEFAULT_GSON，但可随意切换）
JsonKit.configureDefault(new GsonJsonAdapter());
String j1 = JsonKit.toJson(user);

// ② 模板名（走注册表，长期复用）
String j2 = JsonKit.toJson("gson", user);

// ③ 适配器实例（一次性：不注册、不缓存）
String j3 = JsonKit.toJson(new GsonJsonAdapter(), user);

// ④ 适配器工厂（一次性 + 定制，每次 new 即弃，对齐 YshJson 定制路径）
String j4 = JsonKit.toJson(
        () -> new GsonJsonAdapter(b -> b.setDateFormat("yyyy-MM-dd")), user);
```

```java
// fromJson 同样四种重载
User u1 = JsonKit.fromJson(json, User.class);                            // 默认
User u2 = JsonKit.fromJson("gson", json, User.class);                    // 模板名
User u3 = JsonKit.fromJson(new GsonJsonAdapter(), json, User.class);     // 实例
User u4 = JsonKit.fromJson(() -> new GsonJsonAdapter(), json, User.class); // 工厂
```

```java
// 注册表也支持一次性：临时模板，用完即删
registry.register("tmp", new GsonJsonAdapter());
JsonAdapter tmp = registry.useOnce("tmp");            // 取出即删
String j5 = registry.use("tmp", a -> a.toJson(user));  // 作用域封闭：lambda 执行完自动移除
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

### 5. 接入自定义适配器（冷门库 / 公司自研库）

框架基于「接口 + 注册表」设计，**天然支持任何自定义适配器**——冷门库、公司自研库都能接入：

```java
// ① 实现 JsonAdapter 接口（把公司库 API 翻译成门面 5 个方法）
public class CompanyJsonAdapter implements JsonAdapter {
    @Override
    public String toJson(Object obj) { return CompanyJsonLib.toJsonString(obj); }
    @Override
    public <T> T fromJson(String json, Class<T> classOfT) { return CompanyJsonLib.parseObject(json, classOfT); }
    @Override
    public <T> T fromJson(String json, Type type) { return CompanyJsonLib.parseObject(json, type); }
    @Override
    public <T> List<T> fromJsonList(String json, Class<T> classOfT) { return CompanyJsonLib.parseArray(json, classOfT); }
    @Override
    public Map<String, Object> fromJsonMap(String json) { return CompanyJsonLib.parseMap(json); }
}

// ② 注册一行（启动时统一注册，幂等）
JsonAdapterRegistry.getInstance().register("company", new CompanyJsonAdapter());

// ③ 与官方适配器一样使用
JsonAdapter adapter = JsonAdapterRegistry.getInstance().get("company");
String json = adapter.toJson(user);
```

> 接入的好处：公司旧代码不用改，还能和 gson / fastjson / jackson 通过模板名自由切换，
> 业务侧统一走 `JsonAdapter` 门面，零感知底层差异。

## 性能评测（实测）

数据集 1000 条记录，预热 2000 次 + 测量 20000 次（本机 JDK 21）：

| 实现 | 序列化 | 反序列化 | JSON 体积 |
|---|---|---|---|
| GsonJsonAdapter | ~288 μs | ~106 μs | 38,891 bytes |
| FastJsonJsonAdapter | ~21 μs | ~28 μs | 38,891 bytes |
| JacksonJsonAdapter | ~50 μs | ~99 μs | 38,891 bytes |

> fastjson2 序列化最快（相对 gson 快 ~14 倍）；Jackson 排第二。评测代码见 `json-jackson` 模块的 `JsonAdapterBenchmarkTest`。

## 设计亮点

1. **适配器模式**：门面接口与底层实现解耦，换库零改动（对标 JDBC / slf4j）
2. **注册表模式**：命名模板 + fail-fast 防手滑（未注册即抛异常）
3. **依赖隔离**：core 零第三方依赖，各适配器独立模块按需引入
4. **统一注解**：`@JsonField` 一套注解翻译到各底层库，业务代码不感知底层差异
5. **线程安全**：单例注册表由 `ConcurrentHashMap` 支撑，并发压力测试验证

## 构建测试

```bash
mvn test
```

## License

MIT
