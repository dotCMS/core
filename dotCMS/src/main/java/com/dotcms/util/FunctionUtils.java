package com.dotcms.util;

import com.dotmarketing.util.Logger;

import java.util.concurrent.Callable;

/**
 * Encapsulates utils method to extend the Java Functions API
 * @author jsanca
 */
public class FunctionUtils {

    /**
     * The idea behind this method is to concat a consequent callback if value is true.
     * For instance
     * <code>
     *     return ifTrue (ifPasswordChanged(), () -> sendEmail(...))
     * </code>
     * @param value
     * @param callback
     * @param <T>
     * @return boolean
     */
    public static <T> boolean ifTrue (final boolean value, final Callback callback) {

        return OptionalBoolean.of(value).ifTrueGet(callback);
    } // ifTrue.

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
