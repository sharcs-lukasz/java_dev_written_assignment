package org.example.CachingFunction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleNotSynchronizedCacheTest {

    public static void main(String[] args) {
        new SimpleNotSynchronizedCacheTest().parallelRequestsTest();
    }

    /**
     * This test will fail for SimpleNotSynchronizedCache as it is not thread-safe.
     */
    @Test
    void parallelRequestsTest() {
        SimpleNotSynchronizedCache<Integer, String> myCache = new SimpleNotSynchronizedCache<>(Object::toString);
        System.out.println("myCache: " + myCache);
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
        for (int i=0; i < 100; ++i) {
            parallelRequestsTest();
        }
    }
}