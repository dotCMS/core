package com.dotcms.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Encapsulates utils method to extend the Java Functions API
 * @author jsanca
 */
public class FunctionUtils {

    /**
     * Get the value if the condition is true, otherwise return the default value.
     * @param condition
     * @param trueSupplier
     * @param falseSupplier
     * @return
     * @param <T>
     */
    public static <T> T getOrDefault(final boolean condition, final Supplier<T> trueSupplier, final Supplier<T> falseSupplier) {

        return condition?trueSupplier.get():falseSupplier.get();
    } // getOrDefault.

    /**
     * The idea behind this method is to concat a consequent callback if value is true.
            * For instance
     * <code>
     *     return ifTrue (ifPasswordChanged(), () -> sendEmail(.
    * </code>
    * @param condition
     * @param callback
     * @param <T>
     * @return boolean
     */
    public static boolean ifTrue (final boolean condition, final Callback callback) {

        return OptionalBoolean.of(condition).ifTrueGet(callback);
    } // ifTrue.

    /**
     * The idea behind this method is to concat a consequent callback if value is true.
     * For instance
     * <code>
     *     return ifTrue (() -> ifPasswordChanged(), () -> sendEmail(...))
     * </code>
     * @param booleanSupplier just a lazy evaluation
     * @param callback
     * @param <T>
     * @return boolean
     */
    public static boolean ifTrue (final Supplier<Boolean> booleanSupplier, final Callback callback) {

        return ifTrue(booleanSupplier.get(), callback);
    } // ifTrue.

    /**
     * The idea behind this method is to concat a consequent callback if value is true or false.
     * For instance
     * <code>
     *     return ifTrue (ifPasswordChanged(), () -> sendEmail(...), ()-> logAnError())
     * </code>
     * @param condition boolean condition
     * @param onTrueCallback callback to execute when true
     * @param onFalseCallback callback to execute when false
     * @param <T>
     * @return boolean
     */
    public static boolean ifOrElse(final boolean condition, final Callback onTrueCallback, final Callback onFalseCallback) {

        return OptionalBoolean.of(condition).ifTrue(onTrueCallback).orElse(onFalseCallback).get();
    } // ifOrElse.

    public static <T> boolean ifOrElse(final Optional<T> optionalT, final Consumer<T> onTrueConsumer, final Callback onFalseCallback) {

        return OptionalBoolean.of(optionalT.isPresent()).ifTrue(onTrueConsumer, optionalT).orElse(onFalseCallback).get();
    } // ifOrElse.
    /**
     * The idea behind this method is to concat a consequent callback if value is true or false.
     * For instance
     * <code>
     *     return ifTrue (() -> ifPasswordChanged(), () -> sendEmail(...), ()-> logAnError())
     * </code>
     * @param booleanSupplier just a lazy evaluation
     * @param onTrueCallback callback to execute when true
     * @param onFalseCallback callback to execute when false
     * @param <T>
     * @return boolean
     */
    public static boolean ifOrElse(final Supplier<Boolean> booleanSupplier, final Callback onTrueCallback, final Callback onFalseCallback) {

        return ifOrElse(booleanSupplier.get(), onTrueCallback, onFalseCallback);
    } // ifOrElse.

    /**
     * Tries to get the value, if some error happens it is not logged or anything, just quietly returned the defaultValue.
     * @param delegate {@link ReturnableDelegate} delegate to call
     * @param defaultValue T default value in case of error
     * @param <T>
     * @return T
     */
    public static <T> T getValueOrDefault (final ReturnableDelegate<T> delegate, final T defaultValue) {

        T returnValue = defaultValue;

        try {

            returnValue = delegate.execute();
        } catch (Throwable e) {

            returnValue = defaultValue;
        }

        return returnValue;
    } // getValueOrDefault.

    /**
     * Apply a <code>transformer</code> function for each item in a {@link Collection}, and return
     * a new collection with all the items transformed.
     *
     * @param items Original {@link Collection}
     * @param transformer Function to transform each item in the original collection
     * @param <T> Original collection type
     * @param <R> Return collection type
     * @return new Collection with all the items transformed
     */
    public static <T, R> List<R> map(final Collection<T> items, final Function<T, R> transformer){
        return items.stream().map(asset -> transformer.apply(asset)).collect(Collectors.toList());
    }

    /**
     * Just encapsulates a single callback
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * Unique method to be called as a callback
         */
        void call ();
    } // Callback.


} // E:O:F:FunctionUtils.
