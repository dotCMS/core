package com.dotcms.rest.validation;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Simple Precondition checks, influenced heavily by Google's version of same.
 * This implementation adds the ability to specify the exception that is to
 * be thrown, streamlining validation of public API methods by removing the need
 * to wrap the generic exceptions and rethrow as API specific versions.
 *
 * See https://code.google.com/p/guava-libraries/wiki/PreconditionsExplained
 *
 * @todo ggranum: move this class into a 'dotcms common' (or shared) sub-project / package.
 *
 * @author Geoff M. Granum
 */
public class Preconditions {

    public static <T> T checkNotNull(T argument, String message) {
        if(argument == null) {
            throw new NullPointerException(message);
        }
        return argument;
    }

    public static <T> T checkNotNull(
        T argument, Class<? extends RuntimeException> exceptionType,
        String message, Object... messageArgs) {
        if(argument == null) {
            throw newException(message, exceptionType, messageArgs);
        }
        return argument;
    }


    public static String checkNotEmpty(
        String argument, Class<? extends RuntimeException> exceptionType,
        String message, Object... messageArgs) {
        if(StringUtils.isEmpty(argument)) {
            throw newException(message, exceptionType, messageArgs);
        }
        return argument;
    }

    public static <T> T[] checkNotEmpty(T[] argument, Class<? extends RuntimeException> exceptionType, String message, Object... messageArgs) {
        if(argument == null || argument.length == 0) {
            throw newException(message, exceptionType, messageArgs);
        }
        return argument;
    }



    static RuntimeException newException(
        String message, Class<? extends RuntimeException> exceptionType,
        Object... messageArgs) {
        RuntimeException e;
        message = String.format(message, messageArgs);
        if(exceptionType == null) {
            e = new IllegalArgumentException(message);
        } else {
            try {
                Constructor<? extends RuntimeException> constructor = exceptionType.getConstructor(String.class);
                e = constructor.newInstance(message);
            } catch (NoSuchMethodException
                | InvocationTargetException
                | InstantiationException
                | IllegalAccessException e1) {
                throw new RuntimeException("Exception Types provided to Preconditions must have a constructor " +
                                           "that takes a single string argument.", e1);
            }
        }
        return e;
    }


}

 
