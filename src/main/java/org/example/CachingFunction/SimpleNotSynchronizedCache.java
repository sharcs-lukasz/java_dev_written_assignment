package org.example.CachingFunction;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SimpleNotSynchronizedCache<K, V> implements Cache<K, V> {

    private static final int INITIAL_CAPACITY = 4096;
    private final HashMap<K, V> map;

    private final Function<K, V> srcFunc;
    private final AtomicInteger initCounter;

    public SimpleNotSynchronizedCache(Function<K, V> srcFunc) {
        if (srcFunc == null) {
            throw new NullPointerException("Parameter \"srcFunc\" cannot be null.");
        }
        this.srcFunc = srcFunc;
        this.map = new HashMap<>(INITIAL_CAPACITY);
        this.initCounter = new AtomicInteger(0);
    }

    @Override
    public V get(K key) {
        V value = map.get(key);
        if (null == value) {
            value = srcFunc.apply(key);
            map.put(key, value);
            initCounter.getAndIncrement();
        }
        return value;
    }

    public int size() {
        return map.size();
    }

    protected int getInitCounter() {
        return initCounter.get();
    }


    public static void main(String[] args) {
        SimpleNotSynchronizedCache<Integer, String> myCache = new SimpleNotSynchronizedCache<>(i -> i.toString());
        myCache.size();
    }
}
