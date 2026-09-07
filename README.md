# cskit

[English](README.md) | [中文](README_zh.md)

[![CI](https://github.com/CharlesShan-hub/cskit/actions/workflows/ci.yml/badge.svg)](https://github.com/CharlesShan-hub/cskit/actions/workflows/ci.yml)

> CSKit — CharlesShan's Java toolkit.
> Python has [cslib](https://github.com/CharlesShan-hub/cslib), Java is cskit.
> Design philosophy: **pluggable, dependency-isolated** — a unified facade, switchable underlying implementations.

## 📦 Sub-frameworks

| Sub-framework | Description | Docs |
|---|---|---|
| **json** | Unified JSON facade: one API, any underlying library (Gson / Fastjson2 / Jackson), pluggable adapters + `@JsonField` annotation | [doc/json.md](doc/json.md) |

> More sub-frameworks (yaml / xml / http / ...) planned 🌱

## 🏗️ Module Layout

```
cskit/
├── pom.xml               ← aggregator
├── json-core/            ← facade interface + registry + @JsonField (zero third-party deps)
├── json-gson/            ← Gson adapter + annotation translation layer
├── json-fastjson/        ← Fastjson2 adapter
└── json-jackson/         ← Jackson adapter + annotation translation layer
```

**Dependency isolation principle**: the core module has zero third-party dependencies;
pull only the adapter module for the library you want — other libs never enter your classpath.

## 🚀 Quick Start

```xml
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

```java
JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance()
        .register("gson", new GsonJsonAdapter())
        .register("fastjson", new FastJsonJsonAdapter());

JsonAdapter adapter = registry.get("gson");   // switch library by changing this line
String json = adapter.toJson(user);
User u = adapter.fromJson(json, User.class);
```

Full usage, annotations, benchmarks → **[doc/json.md](doc/json.md)**

## 📚 Docs

- [json sub-framework](doc/json.md) | [中文文档](doc/json_zh.md)

## License

MIT
