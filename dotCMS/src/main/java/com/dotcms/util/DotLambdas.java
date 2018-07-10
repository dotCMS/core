package com.dotcms.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DotLambdas {

    /**
     * Just casting of a Predicate.
     * @param predicate
     * @param <T>
     * @return Predicate
     */
    public static <T> Predicate<T> of(final Predicate<T> predicate){
        return predicate;
    }

    /**
     * Just casting of a Consumer
     * @param consumer
     * @param <T>
     * @return Consumer
     */
    public static <T> Consumer<T> of(final Consumer<T> consumer){
        return consumer;
    }

    /**
     * Just casting of a Supplier
     * @param supplier
     * @param <T>
     * @return Supplier
     */
    public static <T> Supplier<T> of(final Supplier<T> supplier){
        return supplier;
    }

    /**
     * Just casting a Function
     * @param function
     * @param <T>
     * @param <R>
     * @return Function
     */
    public static <T, R> Function<T, R> of(final Function<T, R> function){
        return function;
    }

    /**
     * Negate the predicate.
     * @param predicate
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> not(final Predicate<T> predicate) {
        return predicate.negate();
    }

}
