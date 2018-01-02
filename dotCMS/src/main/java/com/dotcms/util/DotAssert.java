package com.dotcms.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Handles assertions.
 * @author jsanca
 */
public class DotAssert {

    /**
     * Assert true, throws IllegalArgumentException if the expression is false.
     *
     * <pre >DotAssert.isTrue(!contentletAPI.canLock(contentlet, user), "Can not lock the content" );</pre>
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
    public static <T extends Throwable>  void isTrue(final boolean expression, final Supplier<String> errorMessage,
                              final Class<T> errorClazz) throws T {

        if (!expression) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // isTrue.

    /**
     * Assert true, throws RuntimeException if the expression is false.
     *
     * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), CannotLockContent.class, ()->"Can not lock the content");</pre>
     * @param expression boolean expression to test
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(final boolean expression,
                              final Class<? extends RuntimeException> errorClazz,
                              final Supplier<String> errorMessage) {

        if (!expression) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // isTrue.

    //////////
    // is NULL

    /**
     * Assert isNull, throws IllegalArgumentException if the parameter is not null.
     *
     * <pre >DotAssert.isNull(user, "User must be null" );</pre>
     * @param parameter Object parameter to test if it is null
     * @param errorMessage String message to pass to the IllegalArgumentException
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isNull(final Object parameter, final String errorMessage) {

        isTrue(null == parameter, errorMessage);
    } // isNull.

    /**
     * Assert isNull, throws IllegalArgumentException if the parameter is not null.
     *
     * <pre >DotAssert.isNull(user, ()->"User must be null" );</pre>
     * @param parameter Object parameter to test if it is null
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isNull(final Object parameter, final Supplier<String> errorMessage) {

        isTrue(null == parameter, errorMessage);
    } // isNull.

    /**
     * Assert isNull, throws Custom Throwable if the parameter is not null.
     *
     * <pre >DotAssert.isNull(user, ()->"User must be null", UserMustBeNullException.class );</pre>
     * @param parameter Object parameter to test if it is null
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     */
    public static <T extends Throwable>  void isNull(final Object parameter, final Supplier<String> errorMessage,
                              final Class<T> errorClazz) throws T {

        isTrue(null == parameter, errorMessage, errorClazz);
    } // isNull.

    /**
     * Assert isNull, throws RuntimeException if the parameter is not null.
     *
     * <pre >DotAssert.isNull(user, UserMustBeNullException.class, ()->"User must be null");</pre>
     * @param parameter Object parameter to test if it is null
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void isNull(final Object parameter,
                              final Class<? extends RuntimeException> errorClazz,
                              final Supplier<String> errorMessage) {

       isTrue(null == parameter, errorClazz, errorMessage);
    } // isNull.

    //////////
    // is NOT NULL

    /**
     * Assert notNull, throws IllegalArgumentException if the parameter is null.
     *
     * <pre >DotAssert.notNull(user, "User must be not null" );</pre>
     * @param parameter Object parameter to test if it is not null
     * @param errorMessage String message to pass to the IllegalArgumentException
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void notNull(final Object parameter, final String errorMessage) {

        isTrue(null != parameter, errorMessage);
    } // notNull.

    /**
     * Assert notNull, throws IllegalArgumentException if the parameter is  null.
     *
     * <pre >DotAssert.notNull(user, ()->"User must be not null" );</pre>
     * @param parameter Object parameter to test if it is not null
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void notNull(final Object parameter, final Supplier<String> errorMessage) {

        isTrue(null != parameter, errorMessage);
    } // notNull.

    /**
     * Assert notNull, throws Custom Throwable if the parameter is null.
     *
     * <pre >DotAssert.notNull(user, ()->"User must be not null", UserMustBeNullException.class );</pre>
     * @param parameter Object parameter to test if it is not null
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static <T extends Throwable>  void notNull(final Object parameter, final Supplier<String> errorMessage,
                              final Class<T> errorClazz) throws T {

        isTrue(null != parameter, errorMessage, errorClazz);
    } // notNull.

    /**
     * Assert notNull, throws RuntimeException if the parameter is null.
     *
     * <pre >DotAssert.notNull(user, UserMustBeNullException.class, ()->"User must be  notnull");</pre>
     * @param parameter Object parameter to test if it is not null
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void notNull(final Object parameter,
                              final Class<? extends RuntimeException> errorClazz,
                              final Supplier<String> errorMessage) {

        isTrue(null != parameter, errorClazz, errorMessage);
    } // notNull.


    ////////
    // NOT EMPTY

    /**
     * Assert notEmpty, throws IllegalArgumentException if the array is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(array, "Array is empty" );</pre>
     * @param array   Object array
     * @param message String message to pass to the IllegalArgumentException
     */
    public static void notEmpty(final Object[] array,
                                final String message) {

        if (null == array || 0 == array.length) {
            throw new IllegalArgumentException(message);
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws IllegalArgumentException if the array is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(array, ()->"Array is empty" );</pre>
     * @param array   Object array
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Object[] array,
                                final Supplier<String> errorMessage) {

        if (null == array || 0 == array.length) {
            throw new IllegalArgumentException(errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws Custom Throwable if the array is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(array, ()->"Array is empty", ArrayEmptyException.class );</pre>
     * @param array   Object array
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     */
    public static <T extends Throwable> void notEmpty(final Object[] array, final Supplier<String> errorMessage,
                              final Class<T> errorClazz) throws T {

        if (null == array || 0 == array.length) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws RuntimeException if the array is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(array, ArrayEmptyException.class, ()->"Array is empty");</pre>
     * @param array   Object array
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Object[] array,
                              final Class<? extends RuntimeException> errorClazz,
                              final Supplier<String> errorMessage) {

        if (null == array || 0 == array.length) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.

    ///

    /**
     * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, "Collection is empty" );</pre>
     * @param collection   Collection
     * @param message String message to pass to the IllegalArgumentException
     */
    public static void notEmpty(final Collection collection,
                                final String message) {

        if (null == collection || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, ()->"Collection is empty" );</pre>
     * @param collection   Collection
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Collection collection,
                                final Supplier<String> errorMessage) {

        if (null == collection || collection.isEmpty()) {
            throw new IllegalArgumentException(errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws Custom Throwable if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, ()->"Collection is empty"  );</pre>
     * @param collection   Collection
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     */
    public static <T extends Throwable> void notEmpty(final Collection collection, final Supplier<String> errorMessage,
                                                      final Class<T> errorClazz) throws T {

        if (null == collection || collection.isEmpty()) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws RuntimeException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, CollectionEmptyException.class, ()->"Collection is empty");</pre>
     * @param collection   Collection
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Collection collection,
                                final Class<? extends RuntimeException> errorClazz,
                                final Supplier<String> errorMessage) {

        if (null == collection || collection.isEmpty()) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.


    ///

    /**
     * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, "Collection is empty" );</pre>
     * @param collection   Map
     * @param message String message to pass to the IllegalArgumentException
     */
    public static void notEmpty(final Map collection,
                                final String message) {

        if (null == collection || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, ()->"Collection is empty" );</pre>
     * @param collection   Map
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Map collection,
                                final Supplier<String> errorMessage) {

        if (null == collection || collection.isEmpty()) {
            throw new IllegalArgumentException(errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws Custom Throwable if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, ()->"Collection is empty"  );</pre>
     * @param collection   Map
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
     */
    public static <T extends Throwable> void notEmpty(final Map collection, final Supplier<String> errorMessage,
                                                      final Class<T> errorClazz) throws T {

        if (null == collection || collection.isEmpty()) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.

    /**
     * Assert notEmpty, throws RuntimeException if the collection is null or empty (zero items).
     *
     * <pre >DotAssert.notEmpty(list, CollectionEmptyException.class, ()->"Collection is empty");</pre>
     * @param collection   Map
     * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
     * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
     */
    public static void notEmpty(final Map collection,
                                final Class<? extends RuntimeException> errorClazz,
                                final Supplier<String> errorMessage) {

        if (null == collection || collection.isEmpty()) {
            throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
        }
    } // notEmpty.
} // E:O:F:DotAssert.
