package com.dotcms.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.dotmarketing.util.Logger;

/**
 * Encapsulates util methods to perform reflection, such as create a new
 * instance without throwing exception (null in case of error), get the types
 * for an array, soon.
 *
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
public class ReflectionUtils implements Serializable {

	/**
	 * Creates a new instance avoiding to throw any exception, null in case it
	 * can not be create (if an exception happens). This implementation is based
	 * on a class name
	 *
	 * Keep in mind you have to cast the object returned.
	 *
	 * @param className
	 *            - {@link String}
	 * @return Object
	 */
    public static final Object newInstance (final String className) {

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
	 * Creates a new instance avoiding to throw any exception, null in case it
	 * can not be create (if an exception happens).
	 * 
	 * @param clazz
	 *            - {@link Class}
	 * @param <T>
	 * @return T
	 */
    public static final <T> T newInstance (final Class<T> clazz) {

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
	 * Creates a new instance avoiding to throw any exception, null in case it
	 * can not be create (if an exception happens). This approach is based on a
	 * constructor with many arguments, keep in mind the method can not find a
	 * contructor to match with the arguments, null will be returned.
	 *
	 * @param clazz
	 *            - {@link Class}
	 * @return Object
	 */
    public static final <T> T newInstance (final Class<T> clazz,
                                           final Object... arguments) {

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

	/**
	 * Get the types of an array, you can pass an array or a comma separated
	 * arguments.
	 *
	 * @param array
	 *            - {@link Object}
	 * @return array of Class
	 */
    public static final Class<?> [] getTypes (final Object... array) {

        Class<?> [] parameterTypes = null;

        if (null != array) {

            parameterTypes = new Class[array.length];
            for (int i = 0; i < array.length; ++i) {

                parameterTypes[i] = array[i].getClass();
            }
        }

        return parameterTypes;
    } // getTypes.

    /**
     * Get the {@link Class} for the type, if it is {@link ParameterizedType} will
     * return the raw type.
     * @param type {@link Type}
     * @return Class
     */
    public static final Class<?> getClassFor (final Type type) {

        return  type instanceof ParameterizedType?
                (Class<?>) ParameterizedType.class.cast(type).getRawType():
                (Class<?>) type;
    }
    /**
     * Get a {@link Class} based on a name, null in case of error.
     * @param className {@link String}
     * @return Class
     */
    public static final Class<?> getClassFor (final String className) {

        Class<?> clazz = null;

        try {

            clazz =
                    Class.forName(className);
        } catch (ClassNotFoundException e) {

            clazz = null;
        }

        return clazz;
    } // getClassFor.

    /**
     * Get the interfaces for a clazz
     * @param clazz {@link Class}
     * @return Class array
     */
    public static final Class[] getInterfaces (final Class<?> clazz) {

        return clazz.getInterfaces();
    } // getInterfaces.

    /**
     * Get the first interfaces, null if not implement any
     * @param clazz {@link Class}
     * @return Class
     */
    public static final Class getFirstInterface (final Class<?> clazz) {

        final Class[] interfaces = getInterfaces(clazz);
        return null != interfaces && interfaces.length > 0? interfaces[0]:null;
    } // getFirstInterface.



} // E:O:F:ReflectionUtils.
