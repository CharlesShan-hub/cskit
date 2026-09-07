package top.cskit.json.jackson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;
import top.cskit.json.jackson.JacksonJsonAdapter;
import top.cskit.json.testkit.Animal;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Multi-framework benchmark suite: correctness comparison + simple
 * performance benchmark.
 *
 * <p>Subjects: gson / fastjson2 / jackson, through the unified
 * {@link JsonAdapter} facade.
 * <ul>
 *   <li>Correctness: serialization/deserialization result consistency</li>
 *   <li>Performance: serialize and deserialize latency (with warm-up to reduce JIT impact)</li>
 * </ul>
 *
 * <p>Note: this is a simple benchmark for trend reference only; use JMH for
 * rigorous benchmarking.
 */
class JsonAdapterBenchmarkTest {

    /** Subjects under test. */
    static final List<JsonAdapter> ADAPTERS = List.of(
            new GsonJsonAdapter(),
            new FastJsonJsonAdapter(),
            new JacksonJsonAdapter()
    );

    /** Dataset: a zoo of animals (realistic shape: List&lt;Bean&gt;, entity reused from Animal). */
    static final List<Animal> ZOO = buildZoo(1000);

    static final int WARMUP_ROUNDS = 2000;   // warm-up rounds (trigger JIT)
    static final int MEASURE_ROUNDS = 20000; // measured rounds

    static List<Animal> buildZoo(int size) {
        List<Animal> zoo = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            zoo.add(new Animal("Animal-" + i, 1938 + i % 80));
        }
        return zoo;
    }

    // region 1. Correctness

    @Test
    @DisplayName("Correctness: complete serialization fields + consistent deserialization")
    void correctness() {
        for (JsonAdapter adapter : ADAPTERS) {
            String name = adapter.getClass().getSimpleName();

            // Serialization: no field loss
            String json = adapter.toJson(ZOO.get(0));
            System.out.println("[" + name + "] serialize sample: " + json);
            assertEquals(true, json.contains("Animal-0"), name + " serialized should contain name");
            assertEquals(true, json.contains("1938"), name + " serialized should contain birthYear");

            // Deserialization: data restored
            Animal back = adapter.fromJson(json, Animal.class);
            assertEquals("Animal-0", back.getName(), name + " deserialized name consistent");
            assertEquals(1938, back.getBirthYear(), name + " deserialized birthYear consistent");

            // Big-list round trip: full check
            String bigJson = adapter.toJson(ZOO);
            List<Animal> bigBack = adapter.fromJsonList(bigJson, Animal.class);
            assertEquals(ZOO.size(), bigBack.size(), name + " big list size consistent");
            assertEquals(ZOO.get(999).getName(), bigBack.get(999).getName(), name + " big list last item consistent");
            System.out.println("[" + name + "] correctness: 1000 items round-trip without loss\n");
        }
    }

    // endregion

    // region 2. Performance (simple benchmark)

    @Test
    @DisplayName("Performance: serialize / deserialize latency comparison (with warm-up)")
    void performance() {
        String jsonCache = null;

        System.out.println("dataset: " + ZOO.size() + " records, warmup " + WARMUP_ROUNDS
                + " rounds, measure " + MEASURE_ROUNDS + " rounds\n");
        System.out.printf("%-22s %-14s %-14s %s%n", "impl", "serialize(ns)", "deserialize(ns)", "JSON size");
        System.out.println("-".repeat(72));

        for (JsonAdapter adapter : ADAPTERS) {
            String name = adapter.getClass().getSimpleName();

            // Warm-up: let JIT fully compile
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                adapter.toJson(ZOO);
            }

            // Measure serialization
            long serStart = System.nanoTime();
            for (int i = 0; i < MEASURE_ROUNDS; i++) {
                jsonCache = adapter.toJson(ZOO);
            }
            long serCost = (System.nanoTime() - serStart) / MEASURE_ROUNDS;

            // Warm-up deserialization
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                adapter.fromJsonList(jsonCache, Animal.class);
            }

            // Measure deserialization
            long desStart = System.nanoTime();
            for (int i = 0; i < MEASURE_ROUNDS; i++) {
                adapter.fromJsonList(jsonCache, Animal.class);
            }
            long desCost = (System.nanoTime() - desStart) / MEASURE_ROUNDS;

            System.out.printf("%-22s %-14d %-14d %d bytes%n",
                    name, serCost, desCost, jsonCache.getBytes().length);
        }
        System.out.println("\nnote: per-call latency (nanoseconds), lower is better; "
                + "data is a whole 1000-record list serialized/deserialized.");
    }

    // endregion
}
