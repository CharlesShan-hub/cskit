package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
import top.cskit.json.fastjson.FastJsonJsonAdapter;
import top.cskit.json.gson.GsonJsonAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册表单例的线程安全验证
 * <p>
 * 单例 = 全局共享 → 必须线程安全。验证三个场景：
 * 1. 并发注册：N 个线程同时 register 不同模板，不丢、不串
 * 2. 并发读：注册完成后 N 个线程同时 get，全部拿到正确适配器
 * 3. 读写并发：一边注册一边读取，无异常、结果一致
 */
class JsonAdapterRegistryConcurrencyTest {

    @Test
    @DisplayName("并发注册 + 并发读取：不丢数据、无异常")
    void concurrentRegisterAndGet() throws Exception {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        int threads = 16;
        int rounds = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int r = 0; r < rounds; r++) {
                        // 每个线程注册自己的模板名（含线程号，互不冲突）
                        String name = "tpl-" + idx;
                        registry.register(name, new GsonJsonAdapter());
                        // 立刻读回来验证一致
                        JsonAdapter got = registry.get(name);
                        if (!(got instanceof GsonJsonAdapter)) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                }
            }));
        }

        startGate.countDown(); // 同时放行 16 个线程
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        System.out.println("并发注册/读取完成, 错误数: " + errors.get());
        assertEquals(0, errors.get(), "并发下不应出现数据丢失或串号");

        // 最终一致性：每个模板名都能读到
        for (int i = 0; i < threads; i++) {
            assertTrue(registry.contains("tpl-" + i), "模板 tpl-" + i + " 应已注册");
        }
        System.out.println("16 个线程 × 200 轮 并发注册/读取全部通过");
    }

    @Test
    @DisplayName("读写并发：注册与读取同时进行，get 始终拿到已注册的值")
    void readWhileWrite() throws Exception {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("concurrent-gson", new GsonJsonAdapter());

        int rounds = 500;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();

        // 4 个读线程：不断 get 已注册的适配器
        List<Future<?>> readers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            readers.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int r = 0; r < rounds; r++) {
                        JsonAdapter a = registry.get("concurrent-gson");
                        if (!(a instanceof GsonJsonAdapter)) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                }
            }));
        }

        // 4 个写线程：反复替换同一模板（模拟运行时扩展）
        List<Future<?>> writers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            writers.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int r = 0; r < rounds; r++) {
                        registry.register("concurrent-gson", new GsonJsonAdapter());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : readers) {
            f.get(30, TimeUnit.SECONDS);
        }
        for (Future<?> f : writers) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        System.out.println("读写并发完成, 错误数: " + errors.get());
        assertEquals(0, errors.get(), "读线程永远读到已注册的值");
        System.out.println("4 读 + 4 写 × 500 轮 并发通过");
    }
}
