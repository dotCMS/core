package com.dotcms.util;

import java.util.function.Supplier;

/**
 * Handles assertions.
 * @author jsanca
 */
public class DotAssert {

    /**
     * Assert true, throws IllegalArgumentException if the expression is false.
     *
     * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), "Can not lock the content" );</pre>
     * @param expression boolean expression to test
     * @param errorMessage String message to pass to the IllegalArgumentException
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(final boolean expression, final String errorMessage) {

        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    } // isTrue.

    /**
     * Assert true, throws IllegalArgumentException if the expression is false.
     *
     * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), ()->"Can not lock the content" );</pre>
     * @param expression boolean expression to test
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(final boolean expression, final Supplier<String> errorMessage) {

        if (!expression) {
            throw new IllegalArgumentException(errorMessage.get());
        }
    } // isTrue.

    /**
     * Assert true, throws Custom Throwable if the expression is false.
     *
     * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), ()->"Can not lock the content", CannotLockContent.class );</pre>
     * @param expression boolean expression to test
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(final boolean expression, final Supplier<String> errorMessage,
                              final Class<Throwable> errorClazz) throws Throwable {

        if (!expression) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // isTrue.

} // E:O:F:DotAssert.
