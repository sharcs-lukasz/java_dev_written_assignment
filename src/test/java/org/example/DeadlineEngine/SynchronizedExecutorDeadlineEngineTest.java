package org.example.DeadlineEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizedExecutorDeadlineEngineTest {

    private SynchronizedExecutorDeadlineEngine engine;

    @BeforeEach
    void setup() {
        engine = new SynchronizedExecutorDeadlineEngine();
    }

    @Test
    void scheduleReturnsErrorWhenWrongParamTest() {
//        assertThrows(IllegalArgumentException.class, () -> { engine.schedule(-1); });
        assertEquals(-1, engine.schedule(-1L));
    }

    @Test
    void scheduleReturnsUniqueScheduleIdTest() {
        HashSet<Long> set = new HashSet<Long>();
        final int N = 10;
        for (int i=0; i < N; ++i) {
            set.add(engine.schedule(100));
        }
        assertEquals(N, set.size());
    }

    @Test
    void scheduleIncreasesSizeTest() {
        final int N = 10;
        for (int i=0; i < N; ++i) {
            engine.schedule(100);
        }
        assertEquals(N, engine.size());
    }

    @Test
    void cancelNoopWhenWrongIdTest() {
        final long invalidId = 0L;
        long id = engine.schedule(100);
        assertEquals(1, engine.size());
        assertFalse(engine.cancel(invalidId));
        assertEquals(1, engine.size());
    }

    @Test
    void cancelDecreasesSizeTest() {
        long id = engine.schedule(100);
        assertEquals(1, engine.size());
        assertTrue(engine.cancel(id));
        assertEquals(0, engine.size());
    }

    @Test
    void randomCancelOrderDecreasesSizeTest() {
        HashMap<Long, Long> map = new HashMap<>();
        final int N = 10;
        for (int i=0; i < N; ++i) {
            map.put(engine.schedule(100), 100L);
        }
        assertEquals(N, engine.size());
        int size = N;
        for (long id: map.keySet()) {
            engine.cancel(id);
            assertEquals(--size, engine.size());
        }
        assertEquals(0, engine.size());
    }

    @Test
    void pollErrorsWhenWrongParamsTest() {
        engine.schedule(100);
        assertEquals(0, engine.poll(-1, null, 1));
        assertEquals(0, engine.poll(101, null, -1));
        assertEquals(0, engine.poll(101, null, 0));
        assertEquals(0, engine.poll(-1, null, -1));
    }

    @Test
    void pollRemovesOnlyExpiredDeadlinesTest() {
        engine.schedule(100);
        assertEquals(1, engine.size());
        assertEquals(0, engine.poll(99, null, 1));
        assertEquals(1, engine.size());
        assertEquals(1, engine.poll(100, null, 1));
        assertEquals(0, engine.size());
    }

    @Test
    void pollRemovesMaxPollExpiredDeadlinesAtMostTest() {
        final int N = 10;
        final int maxPoll = 5;
        for (int i=0; i < N; ++i) {
            engine.schedule(100);
        }
        assertEquals(N, engine.size());
        assertEquals(maxPoll, engine.poll(100, null, maxPoll));
        assertEquals(N-maxPoll, engine.size());
    }

    @Test
    void pollDoesNotRemoveFutureDeadlinesTest() {
        final int N = 10;
        for (int i=0; i < N; ++i) {
            engine.schedule(100 + i * 10);
        }
        assertEquals(N, engine.size());
        assertEquals(3, engine.poll(120, null, N));
        assertEquals(N-3, engine.size());
    }

    @Test
    void simpleUsageTest() {
        System.out.println("engine [" + engine.size() + "]: " + engine);
        engine.poll(1, null, 1);
        engine.poll(1, id -> System.out.println("this should not be triggered, id: " + id), 1);
        assertEquals(0, engine.size());

        long id1 = engine.schedule(60L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(1, engine.size());
        engine.poll(59, id -> System.out.println("triggered schedule with id " + id), 1);
        assertEquals(1, engine.size());

        long id2 = engine.schedule(90L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());
        engine.poll(89, id -> System.out.println("triggered schedule with id " + id), 1);
        assertEquals(1, engine.size());

        long id3 = engine.schedule(110L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.cancel(id2);
        assertEquals(1, engine.size());

        long id4 = engine.schedule(120L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.poll(120, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(0, engine.size());

        long id5 = engine.schedule(150L);
        long id6 = engine.schedule(151L);
        long id7 = engine.schedule(152L);
        long id8 = engine.schedule(153L);
        long id9 = engine.schedule(154L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(5, engine.size());

        engine.poll(155, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.poll(155, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(0, engine.size());
    }

    @Test
    void multiThreadingTest() {
        ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

        Runnable generatorRunnable = new Runnable() {
            @Override
            public void run() {
                long threadId = Thread.currentThread().getId();
                int counter = 0;
                System.out.println(Thread.currentThread().getName() + " [" + Thread.currentThread().getId() + "]: Starting generator thread..");
                for (long deadline=100L; deadline < 10_000L; deadline += 100L ) {
                    long scheduleId = engine.schedule(System.currentTimeMillis() + deadline + threadId);
                    queue.add(scheduleId);
                    ++counter;
                    try {
                        Thread.sleep(10);
                    } catch(InterruptedException e) {
                        System.out.println(Thread.currentThread().getName() + " [" + Thread.currentThread().getId() + "] Exception caught: " + e);
                    }
                }
                System.out.println(Thread.currentThread().getName() + " [" + Thread.currentThread().getId() + "]: Generator thread completed. Number of deadlines scheduled: " + counter);
            }
        };
        Runnable pollingRunnable = new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                try {
                    Thread.sleep(50);
                    while (engine.size() > 0) {
                        System.out.println("engine size: " + engine.size());
                        counter += engine.poll(System.currentTimeMillis(), id -> System.out.println("triggered schedule with id " + id), 3);
                        Thread.sleep(10);
                    }
                } catch(InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + " [" + Thread.currentThread().getId() + "] Exception caught: " + e);
                }
                System.out.println(Thread.currentThread().getName() + " [" + Thread.currentThread().getId() + "] Polling thread completed. Number of triggered deadlines: " + counter);
            }
        };

        try {
            Thread pollingThread1 = new Thread(pollingRunnable);
            Thread generatorThread1 = new Thread(generatorRunnable);
            Thread generatorThread2 = new Thread(generatorRunnable);
            Thread generatorThread3 = new Thread(generatorRunnable);
            System.out.println("engine [" + engine.size() + "]: " + engine);
            assertEquals(0, engine.size());

            pollingThread1.start();
            generatorThread1.start();
            assertTrue(engine.size() <= 99);

            generatorThread2.start();
            assertTrue(engine.size() <= 2*99);

            generatorThread3.start();
            assertTrue(engine.size() <= 3*99);

            generatorThread1.join();
            assertTrue(engine.size() <= 3*99);

            generatorThread2.join();
            assertTrue(engine.size() <= 3*99);

            generatorThread3.join();
            assertTrue(engine.size() <= 3*99);

            pollingThread1.join();
            assertEquals(0, engine.size());
        } catch(InterruptedException e) {
            System.out.println("Caught exception: " + e);
        }
        System.out.println("engine [" + engine.size() + "]: " + engine);
    }
}
