package top.cskit.json.jackson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.jackson.JacksonJsonAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 多 JSON 框架评测套件：正确性对比 + 性能基准（简易 benchmark）
 * <p>
 * 评测对象：gson / fastjson2 / jackson（通过统一 {@link JsonAdapter} 门面）
 * 评测维度：
 * 1. 正确性：序列化/反序列化结果一致性
 * 2. 性能：序列化耗时、反序列化耗时（含预热，降低 JIT 影响）
 * <p>
 * 注意：本测试为简易基准，结论仅供趋势参考；严格基准请使用 JMH。
 */
class JsonAdapterBenchmarkTest {

    /** 评测对象注册 */
    static final List<JsonAdapter> ADAPTERS = List.of(
            new GsonJsonAdapter(),
            new FastJsonJsonAdapter(),
            new JacksonJsonAdapter()
    );

    /** 评测数据集：动物园居民列表（真实业务形态：List<Bean>） */
    static final List<Animal> ZOO = buildZoo(1000);

    static final int WARMUP_ROUNDS = 2000;  // 预热轮数（触发 JIT）
    static final int MEASURE_ROUNDS = 20000; // 正式测量轮数

    static List<Animal> buildZoo(int size) {
        List<Animal> zoo = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            zoo.add(new Animal("Animal-" + i, 1938 + i % 80));
        }
        return zoo;
    }

    public static class Animal {
        private String name;
        private int birthYear;

        public Animal() {
        }

        public Animal(String name, int birthYear) {
            this.name = name;
            this.birthYear = birthYear;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(int birthYear) {
            this.birthYear = birthYear;
        }
    }

    // region 1. 正确性评测

    @Test
    @DisplayName("正确性：序列化字段齐全 + 反序列化数据一致")
    void correctness() {
        for (JsonAdapter adapter : ADAPTERS) {
            String name = adapter.getClass().getSimpleName();

            // 序列化：字段不能丢
            String json = adapter.toJson(ZOO.get(0));
            System.out.println("[" + name + "] 序列化示例: " + json);
            assertEquals(true, json.contains("Animal-0"), name + " 序列化应含 name");
            assertEquals(true, json.contains("1938"), name + " 序列化应含 birthYear");

            // 反序列化：数据要还原（单个对象用 fromJson）
            Animal back = adapter.fromJson(json, Animal.class);
            assertEquals("Animal-0", back.getName(), name + " 反序列化 name 一致");
            assertEquals(1938, back.getBirthYear(), name + " 反序列化 birthYear 一致");

            // 大列表往返：全量校验
            String bigJson = adapter.toJson(ZOO);
            List<Animal> bigBack = adapter.fromJsonList(bigJson, Animal.class);
            assertEquals(ZOO.size(), bigBack.size(), name + " 大列表条数一致");
            assertEquals(ZOO.get(999).getName(), bigBack.get(999).getName(), name + " 大列表末条一致");
            System.out.println("[" + name + "] 正确性: ✅ 1000 条往返无丢失\n");
        }
    }

    // endregion

    // region 2. 性能评测（简易基准）

    @Test
    @DisplayName("性能：序列化 / 反序列化耗时对比（含预热）")
    void performance() {
        String jsonCache = null;

        System.out.println("数据集: " + ZOO.size() + " 条记录, 预热 " + WARMUP_ROUNDS + " 次, 测量 " + MEASURE_ROUNDS + " 次\n");
        System.out.printf("%-22s %-14s %-14s %s%n", "实现", "序列化(ns)", "反序列化(ns)", "JSON大小");
        System.out.println("-".repeat(72));

        for (JsonAdapter adapter : ADAPTERS) {
            String name = adapter.getClass().getSimpleName();

            // 预热：让 JIT 充分编译
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                adapter.toJson(ZOO);
            }

            // 测量序列化
            long serStart = System.nanoTime();
            for (int i = 0; i < MEASURE_ROUNDS; i++) {
                jsonCache = adapter.toJson(ZOO);
            }
            long serCost = (System.nanoTime() - serStart) / MEASURE_ROUNDS;

            // 预热反序列化
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                adapter.fromJsonList(jsonCache, Animal.class);
            }

            // 测量反序列化
            long desStart = System.nanoTime();
            for (int i = 0; i < MEASURE_ROUNDS; i++) {
                adapter.fromJsonList(jsonCache, Animal.class);
            }
            long desCost = (System.nanoTime() - desStart) / MEASURE_ROUNDS;

            System.out.printf("%-22s %-14d %-14d %d bytes%n",
                    name, serCost, desCost, jsonCache.getBytes().length);
        }
        System.out.println("\n说明: 单次耗时(纳秒)，越小越快；数据为 1000 条记录整体序列化/反序列化。");
    }

    // endregion
}
