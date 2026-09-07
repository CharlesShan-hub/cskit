package top.cskit.json.gson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.cskit.json.JsonAdapter;
import top.cskit.json.JsonAdapterRegistry;
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
 * Thread-safety verification of the singleton registry.
 *
 * <p>A singleton is globally shared, so it must be thread-safe. Three
 * scenarios are verified:
 * <ol>
 *   <li>Concurrent registration: N threads register different names, no loss, no mix-up</li>
 *   <li>Concurrent read: after registration, N threads get the correct adapters</li>
 *   <li>Read + write concurrency: registering while reading, no errors, consistent results</li>
 * </ol>
 */
class JsonAdapterRegistryConcurrencyTest {

    @Test
    @DisplayName("Concurrent register + get: no data loss, no exceptions")
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
                        // Each thread registers its own name (thread-id in it, no conflicts)
                        String name = "tpl-" + idx;
                        registry.register(name, new GsonJsonAdapter());
                        // Read it back immediately to verify consistency
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

        startGate.countDown(); // release all 16 threads at once
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        System.out.println("concurrent register/get done, errors: " + errors.get());
        assertEquals(0, errors.get(), "no data loss or mix-up under concurrency");

        // Final consistency: every template name is readable
        for (int i = 0; i < threads; i++) {
            assertTrue(registry.contains("tpl-" + i), "template tpl-" + i + " should be registered");
        }
        System.out.println("16 threads x 200 rounds concurrent register/get all passed");
    }

    @Test
    @DisplayName("Read while write: get always returns a registered value")
    void readWhileWrite() throws Exception {
        JsonAdapterRegistry registry = JsonAdapterRegistry.getInstance();
        registry.register("concurrent-gson", new GsonJsonAdapter());

        int rounds = 500;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();

        // 4 reader threads: repeatedly get the registered adapter
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

        // 4 writer threads: repeatedly replace the same template (runtime extension)
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

        System.out.println("read/write concurrency done, errors: " + errors.get());
        assertEquals(0, errors.get(), "readers always get a registered value");
        System.out.println("4 readers + 4 writers x 500 rounds passed");
    }
}
