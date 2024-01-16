package org.example.CachingFunction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Thread-safe cache that returns values of type V associated with keys of type K from an internal collection, if the
 * value is cached. Otherwise, it calls a provided function to get/calculate the value, caches it and returns it.
 */
public class SynchronizedCache<K, V> implements Cache<K, V> {

    private static final int INITIAL_CAPACITY = 4096;
    private final ConcurrentHashMap<K, V> map;
    private final Object[] locks;

    private final Function<K, V> srcFunc;

    /**
     * This field is for testing only. Incremented each time `srcFunc` is called.
     */
    private final AtomicInteger initCounter;

    /**
     * Constructor for the thread-safe SynchronizedCache.
     * @param srcFunc Function to be called on a cache miss to calculate value (V) associated with a key (K).
     */
    public SynchronizedCache(Function<K, V> srcFunc) {
        if (srcFunc == null) {
            throw new NullPointerException("Parameter \"srcFunc\" cannot be null.");
        }
        this.srcFunc = srcFunc;
        this.map = new ConcurrentHashMap<>(INITIAL_CAPACITY);
        this.locks = new Object[255];
        for (int i = 0; i < locks.length; ++i) {
            locks[i] = new Object();
        }
        this.initCounter = new AtomicInteger(0);
    }

    /**
     * Get value of type V associated with the provided key of type K from the internal collection if the value is
     * cached. Otherwise, calls the provided Function<K, V> to calculate the value, cache it and return.
     * @param key Key of type K associated with the requested value.
     * @return Requested value of type V associated with the provided `key`.
     */
    @Override
    public V get(K key) {
        if (null == key) {
            throw new NullPointerException("Cannot invoke \"Object.hashCode()\" because \"key\" is null");
        }

        final Object lock = locks[(key.hashCode() & 0x7FFFFFFF) % locks.length];
        synchronized (lock) {
            V value = map.get(key);
            if (null == value) {
                value = srcFunc.apply(key);
                if (null == value) {
                    throw new NullPointerException("The provided Function<K,V> returned null for the given \"key\"");
                }
                map.put(key, value);
                initCounter.incrementAndGet();
            }
            return value;
        }
    }

    /**
     * Returns size of the cache.
     * @return Size of the cache.
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * Returns internal metric representing number of times the `srcFunc` was called.
     * Note that this counts successful calls to `srcFunc` which did not result in raising exceptions.
     * @return Number of times the `srcFunc` was called.
     */
    protected int getInitCounter() {
        return initCounter.get();
    }
}
