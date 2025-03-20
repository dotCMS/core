package com.dotcms.telemetry.util;


import java.util.function.Supplier;

/**
 * This is a cache designed to store any results needed by multiple instances of
 * {@link com.dotcms.telemetry.MetricType}. You can utilize a supplier to obtain the data you wish
 * to store. The cache is flushed every 10 minutes.
 *
 * @param <T>
 */
public class MetricCache<T> {

    private final Supplier<T> supplier;
    private T currentValue = null;

    public MetricCache(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (currentValue == null) {
            currentValue = this.supplier.get();
        }

        return currentValue;
    }

    public void flush() {
        currentValue = null;
    }
}
