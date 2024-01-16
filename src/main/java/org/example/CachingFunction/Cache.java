package org.example.CachingFunction;

/**
 * Generic caching interface.
 */
public interface Cache<K, V> {
    V get(K key);
}
