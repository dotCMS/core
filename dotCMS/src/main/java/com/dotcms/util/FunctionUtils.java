package com.dotcms.util;

import java.util.function.Supplier;

/**
 * Encapsulates utils method to extend the Java Functions API
 * @author jsanca
 */
public class FunctionUtils {

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
     * Just encapsulates a single callback
     */
    public interface Callback {

        /**
         * Unique method to be called as a callback
         */
        void call ();
    } // Callback.
} // E:O:F:FunctionUtils.
