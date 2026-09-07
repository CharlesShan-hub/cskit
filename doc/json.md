# cskit-json — Unified JSON Facade Framework

[English](json.md) | [中文](json_zh.md)

> One API, any underlying library, pluggable adapters.
> Business code talks only to the `top.cskit.json.JsonAdapter` interface; pick a template name
> in the registry for the underlying library — **switch libraries with zero code changes**.

## Why this framework?

The Java world has many JSON libraries (Gson, Fastjson, Jackson...), each with trade-offs:

| Library | Pros | Cons |
|---|---|---|
| Gson | Reads fields via reflection, lightweight, stable | Average performance, weak annotations |
| Fastjson2 | Extremely fast (measured 3~14x) | Strict field visibility, 1.x incompatible with JDK17+ |
| Jackson | Feature-rich, big ecosystem, good performance | Verbose config, intrusive annotations |

**Problem**: once business code directly depends on a library, switching libraries means
touching every call site — expensive.

**Solution**: separate the "facade interface" from "underlying adapters" — business code
only depends on `JsonAdapter`; pick a template name in the registry for the library you want.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Business Code                      │
│        depends only on top.cskit.json.JsonAdapter   │
└─────────────────────────┬───────────────────────────┘
                          │ uses
┌─────────────────────────▼───────────────────────────┐
│             JsonAdapterRegistry (registry)           │
│        register("gson", ...) / register("fastjson")  │
└──────┬──────────────────────────┬───────────────────┘
       │                          │
┌──────▼──────┐          ┌────────▼─────────┐
│ json-gson   │          │ json-fastjson    │
│ (Gson adap.)│          │ (Fastjson2 adap.)│
└──────┬──────┘          └────────┬─────────┘
       │                          │
┌──────▼──────────────────────────▼─────────┐
│          json-core (zero 3rd-party deps)   │
│   JsonAdapter + registry + @JsonField      │
└───────────────────────────────────────────┘
```

## Modules

| Module | Responsibility | 3rd-party deps |
|---|---|---|
| `json-core` | Facade interface, registry, `@JsonField`, field resolver | **zero** |
| `json-gson` | Gson adapter + annotation translation layer | gson |
| `json-fastjson` | Fastjson2 adapter | fastjson2 |
| `json-jackson` | Jackson adapter + annotation translation layer | jackson-databind |

> **Dependency isolation**: a project that only wants Gson pulls `json-core` + `json-gson`;
> other libraries never enter its classpath.

## Quick Start

### 1. Add dependencies

```xml
<!-- core + the adapter you want (Gson here) -->
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

### 2. JsonKit — unified API by overloads (one name, four modes)

Same method name, different param types = different usage mode (no API-name explosion):

```java
// ① default adapter (configure once, like YshJson's DEFAULT_GSON but switchable)
JsonKit.configureDefault(new GsonJsonAdapter());
String j1 = JsonKit.toJson(user);

// ② template name (registry long-term reuse)
String j2 = JsonKit.toJson("gson", user);

// ③ adapter instance (one-shot: not registered, not cached)
String j3 = JsonKit.toJson(new GsonJsonAdapter(), user);

// ④ adapter factory (one-shot + customize, new-and-discard like YshJson)
String j4 = JsonKit.toJson(
        () -> new GsonJsonAdapter(b -> b.setDateFormat("yyyy-MM-dd")), user);
```

```java
// fromJson: same four overloads
User u1 = JsonKit.fromJson(json, User.class);                          // default
User u2 = JsonKit.fromJson("gson", json, User.class);                  // template
User u3 = JsonKit.fromJson(new GsonJsonAdapter(), json, User.class);   // instance
User u4 = JsonKit.fromJson(() -> new GsonJsonAdapter(), json, User.class); // factory
```

```java
// Registry one-shot too: temporary template, gone after use
registry.register("tmp", new GsonJsonAdapter());
JsonAdapter tmp = registry.useOnce("tmp");               // take & auto-remove
String j5 = registry.use("tmp", a -> a.toJson(user));      // scoped: removed after lambda
```

### 3. Unified annotation `@JsonField`

One annotation, effective across all adapters (Gson supported; others extend as needed):

```java
public class Employee {
    @JsonField("ename")                // rename field (JSON uses ename)
    private String name;

    @JsonField(serialize = false)      // read-only, hidden on output
    private double cost;

    @JsonField(deserialize = false)    // write-only, reject external input
    private double profit;
}
```

### 4. Add a new library adapter (e.g. Jackson)

```java
public class JacksonJsonAdapter implements JsonAdapter {
    // just implement the 5 interface methods
}
// then register:
registry.register("jackson", new JacksonJsonAdapter());
```

### 5. Plug in a custom adapter (niche / company-internal library)

Built on "interface + registry", the framework **naturally supports any custom adapter**:

```java
// ① Implement JsonAdapter (translate the company lib into the 5 facade methods)
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

// ② Register in one line (idempotent, do it at startup)
JsonAdapterRegistry.getInstance().register("company", new CompanyJsonAdapter());

// ③ Use it like any official adapter
JsonAdapter adapter = JsonAdapterRegistry.getInstance().get("company");
String json = adapter.toJson(user);
```

> Benefit: legacy company code stays untouched, yet can freely switch with
> gson / fastjson / jackson via template names — business code never sees the difference.

## Benchmark (measured)

Dataset: 1000 records, warmup 2000 + measure 20000 rounds (local JDK 21):

| Implementation | Serialize | Deserialize | JSON size |
|---|---|---|---|
| GsonJsonAdapter | ~288 μs | ~106 μs | 38,891 bytes |
| FastJsonJsonAdapter | ~21 μs | ~28 μs | 38,891 bytes |
| JacksonJsonAdapter | ~50 μs | ~99 μs | 38,891 bytes |

> Fastjson2 is fastest to serialize (~14x vs Gson); Jackson is second.
> Benchmark code: `JsonAdapterBenchmarkTest` in the `json-jackson` module.

## Design Highlights

1. **Adapter pattern**: facade decoupled from implementations; switch libraries with zero changes (cf. JDBC / slf4j)
2. **Registry pattern**: named templates + fail-fast (throws on unregistered name)
3. **Dependency isolation**: core has zero 3rd-party deps; pull only the adapter modules you need
4. **Unified annotation**: one `@JsonField` translated across all adapters
5. **Thread safety**: singleton registry backed by `ConcurrentHashMap`, verified by stress tests

## Build & Test

```bash
mvn test
```

## License

MIT
