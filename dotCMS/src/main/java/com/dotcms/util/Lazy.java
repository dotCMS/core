package com.dotcms.util;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
// from : https://dzone.com/articles/be-lazy-with-java-8
public final class Lazy<T> {

    private volatile T value;

    public boolean isComputed() {
        return value != null;
    }

    public void doIfComputed(Consumer<T> consumer) {
        if (value!=null)
            consumer.accept(value);
    }

    public T getOrCompute(Supplier<T> supplier) {
        final T result = value; // Just one volatile read
        return result == null ? maybeCompute(supplier) : result;
    }

    private synchronized T maybeCompute(Supplier<T> supplier) {
        if (value == null) {
            value = requireNonNull(supplier.get());
        }
        return value;
    }

}