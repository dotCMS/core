package com.dotcms.util;

import java.util.function.Supplier;

/**
 * Encapsulates utils method to extend the Java Functions API
 * @author jsanca
 */
public class FunctionUtils { // todo: to make a test

    /**
     * The idea behind this method is to concat a consequent callback if value is true.
            * For instance
     * <code>
     *     return ifTrue (ifPasswordChanged(), () -> sendEmail(...))
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
    public static boolean ifElse (final boolean condition, final Callback onTrueCallback, final Callback onFalseCallback) {

        return OptionalBoolean.of(condition).ifTrue(onTrueCallback).ifFalse(onFalseCallback).get();
    } // ifElse.

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
    public static boolean ifElse (final Supplier<Boolean> booleanSupplier, final Callback onTrueCallback, final Callback onFalseCallback) {

        return ifElse(booleanSupplier.get(), onTrueCallback, onFalseCallback);
    } // ifElse.


    /**
     * Just encapsulates a single callback
     */
    public interface Callback {

        /**
         * Unique method to be called as a callback
         */
        void call ();
    } // Callback.
} // E:O:F:FunctionUtils.
