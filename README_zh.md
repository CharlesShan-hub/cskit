# cskit

[English](README.md) | [中文](README_zh.md)

[![CI](https://github.com/CharlesShan-hub/cskit/actions/workflows/ci.yml/badge.svg)](https://github.com/CharlesShan-hub/cskit/actions/workflows/ci.yml)

> CSKit —— CharlesShan 的 Java 工具库。
> Python 有 [cslib](https://github.com/CharlesShan-hub/cslib)，Java 就是 cskit。
> 设计理念：**可插拔、依赖隔离**——一套统一门面，底层实现按需切换。

## 📦 子框架

| 子框架 | 说明 | 详细文档 |
|---|---|---|
| **json** | 统一 JSON 门面：一套 API，任意底层库（Gson / Fastjson2 / Jackson），可插拔适配器 + `@JsonField` 注解 | [doc/json_zh.md](doc/json_zh.md) |

> 更多子框架（yaml / xml / http / ...）规划中，敬请期待 🌱

## 🏗️ 模块结构

```
cskit/
├── pom.xml               ← 聚合工程
├── json-core/            ← 门面接口 + 注册表 + @JsonField（零第三方依赖）
├── json-gson/            ← Gson 适配器 + 注解翻译层
├── json-fastjson/        ← Fastjson2 适配器
└── json-jackson/         ← Jackson 适配器 + 注解翻译层
```

**依赖隔离原则**：核心模块零第三方依赖；想用哪个底层库，只引对应的适配器模块，
其他库的 jar 不会进入你的 classpath。

## 🚀 快速上手

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

JsonAdapter adapter = registry.get("gson");   // 换库只改这一行
String json = adapter.toJson(user);
User u = adapter.fromJson(json, User.class);
```

完整用法、注解、性能评测见 → **[doc/json_zh.md](doc/json_zh.md)**

## 📚 文档

- [json 子框架详解（中文）](doc/json_zh.md) | [English](doc/json.md)

## License

MIT
