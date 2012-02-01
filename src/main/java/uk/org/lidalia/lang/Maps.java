package uk.org.lidalia.lang;

import java.util.concurrent.ConcurrentMap;

public final class Maps {

    public static <K, V> V putIfAbsentReturningValue(final ConcurrentMap<K, V> map, final K key, final V value) {
        final V alreadyInMap = map.putIfAbsent(key, value);
        final V result;
        if (alreadyInMap == null) {
            result = value;
        } else {
            result = alreadyInMap;
        }
        return result;
    }

    private Maps() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
