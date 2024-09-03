package com.github.sun.foundation.boot.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Cache<K, V> {
    private Map<K, V> cache = Collections.emptyMap();

    public void remove(K key) {
        cache.remove(key);
    }

    public V get(K key) {
        return get(key, () -> null);
    }

    public V get(K key, Supplier<V> func) {
        V value = cache.get(key);
        if (value == null) {
            synchronized (this) {
                value = cache.get(key);
                if (value == null) {
                    value = func.get();
                    Map<K, V> map = new HashMap<>(cache);
                    map.put(key, value);
                    cache = Collections.unmodifiableMap(map);
                }
            }
        }
        return value;
    }
}
