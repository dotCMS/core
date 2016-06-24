package com.dotmarketing.util;


import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Encapsulates util method to perform reflection
 * @author jsanca
 */
public class ReflectionUtils implements Serializable {

    /**
     * create a new instance avoiding any exception, null in case it can not be create.
     * @param className {@link String}
     * @return Object
     */
    public static final Object newInstance (final String className) { // todo: unit test me

        Object o = null;

        try {

            if (null != className) {
                o =
                        newInstance(Class.forName(className));
            }
        } catch (ClassNotFoundException e) {

            if (Logger.isErrorEnabled(ReflectionUtils.class)) {

                Logger.error(ReflectionUtils.class, e.getMessage(), e);
            }
        }

        return o;
    }
    /**
     * create a new instance avoiding any exception, null in case it can not be create.
     * @param clazz {@link Class}
     * @param <T>
     * @return T
     */
    public static final <T> T newInstance (final Class<T> clazz) { // todo: unit test me

        T t = null;

        if (null != clazz) {

            try {

                t = clazz.newInstance();
            } catch (Exception e) {

                if (Logger.isErrorEnabled(ReflectionUtils.class)) {

                    Logger.error(ReflectionUtils.class, e.getMessage(), e);
                }
            }
        }

        return t;
    } // newInstance.


    /**
     * create a new instance avoiding any exception, null in case it can not be create.
     * @param clazz {@link Class}
     * @return Object
     */
    public static final <T> T newInstance (final Class<T> clazz,
                                           final Object... arguments) { // todo: unit test me

        T t = null;
        Constructor<?> constructor = null;
        Class<?> [] parameterTypes = null;

        if (null != clazz) {

            try {

                parameterTypes = getTypes(arguments);
                constructor = clazz.getDeclaredConstructor(parameterTypes);
                t = (T) constructor.newInstance(arguments);
            } catch (Exception e) {

                if (Logger.isErrorEnabled(ReflectionUtils.class)) {

                    Logger.error(ReflectionUtils.class, e.getMessage(), e);
                }
            }
        }

        return t;
    } // newInstance.


    public static final Class<?> [] getTypes (final Object... array) {

        Class<?> [] parameterTypes = null; // todo: unit test me

        if (null != array) {

            parameterTypes = new Class[array.length];
            for (int i = 0; i < array.length; ++i) {

                parameterTypes[i] = array[i].getClass();
            }
        }

        return parameterTypes;
    }

} // E:O:F:ReflectionUtils.
