package org.example.CachingFunction;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizedCacheTest {

    @Test
    void constructorThrowsWhenFuncIsNullTest() {
        assertThrows(NullPointerException.class, () -> { new SynchronizedCache<Integer, String>(null); });
    }

    @Test
    void createsCacheWhenFuncNotNullTest() {
        final SynchronizedCache<Integer, String> cache = new SynchronizedCache<>(String::valueOf);
        assertNotNull(cache);
    }

    @Test
    void throwsExceptionWhenFuncThrowsNullPointerExceptionTest() {
        final SynchronizedCache<Integer, String> cache = new SynchronizedCache<>(i -> null);
        assertThrows(NullPointerException.class, () -> cache.get(1));
    }

    @Test
    void sizeNotChangedWhenFuncThrowsNullPointerExceptionTest() {
        final Function<Integer, String> func = (i) -> {
            if (13 <= i) {
                return null;
            }
            return String.valueOf(i);
        };
        final SynchronizedCache<Integer, String> cache = new SynchronizedCache<>(func);
        assertEquals(0, cache.size());
        for (int i=0; i < 20; ++i) {
            try {
                cache.get(i);
            } catch(NullPointerException e) {
                // Ignored.
            }
        }
        assertEquals(13, cache.size());
    }

    @Test
    void basicSizeAndValueTest() {
        final SynchronizedCache<Integer, String> cache = new SynchronizedCache<>(String::valueOf);
        assertEquals(0, cache.size());

        final int[] arr =  { 1, 3, 21, 5, 7, 8, 15, 2, 11, 21, 20,  2,  9, 21,  5, 17, 13,  6, 14, 16};
        final int[] size = { 1, 2,  3, 4, 5, 6,  7, 8,  9,  9, 10, 10, 11, 11, 11, 12, 13, 14, 15, 16};
        for (int i=0; i < arr.length; ++i) {
            assertEquals(String.valueOf(arr[i]), cache.get(arr[i]));
            assertEquals(size[i], cache.size());
        }
        assertEquals(16, cache.size());
        assertEquals(16, cache.getInitCounter());
    }

    @Test
    void parallelRequestsTest() {
        SynchronizedCache<Integer, String> myCache = new SynchronizedCache<>(Object::toString);
        Thread thread1 = new Thread(() -> {
            int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            for (int x: numbers) {
                myCache.get(x);
            }
        });
        Thread thread2 = new Thread(() -> {
            int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
            for (int x: numbers) {
                myCache.get(x);
            }
        });
        Thread thread3 = new Thread(() -> {
            int[] numbers = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
            for (int x: numbers) {
                myCache.get(x);
            }
        });
        Thread thread4 = new Thread(() -> {
            int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
            for (int x: numbers) {
                myCache.get(x);
            }
        });
        assertEquals(0, myCache.size());
        try {
            thread1.start();
            thread2.start();
            thread3.start();
            thread4.start();
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
            assertEquals(30, myCache.size());
            assertEquals(30, myCache.getInitCounter());
        } catch(InterruptedException e) {
            System.err.println("Caught InterruptedException");
        }
    }

    @Test
    void parallelRequestsTestRepeated() {
        for (int i=0; i < 1_000; ++i) {
            parallelRequestsTest();
        }
    }

    @Test
    void parallelRequestsTest2() {
        final int MAX_REQUEST = 100_000;
        final int THREADS_COUNT = 500;
        SynchronizedCache<Integer, String> myCache = new SynchronizedCache<>(Object::toString);

        Runnable runnable = () -> {
            for (int x=0; x < MAX_REQUEST; ++x) {
                myCache.get(x);
            }
        };
        Thread[] threads = new Thread[THREADS_COUNT];
        for (int indx=0; indx < threads.length; ++indx) {
            threads[indx] = new Thread(runnable);
        }
        assertEquals(0, myCache.size());
        assertEquals(0, myCache.getInitCounter());

        try {
            for (int indx=0; indx < threads.length; ++indx) {
                threads[indx].start();
            }

            for (int indx=0; indx < threads.length; ++indx) {
                threads[indx].join();
            }
            System.out.println("cache:");
            System.out.println("- size: " + myCache.size());
            System.out.println("- init counter: " + myCache.getInitCounter());

            assertEquals(MAX_REQUEST, myCache.size());
            assertEquals(MAX_REQUEST, myCache.getInitCounter());
        } catch(InterruptedException e) {
            System.err.println("Caught InterruptedException");
        }
    }
}