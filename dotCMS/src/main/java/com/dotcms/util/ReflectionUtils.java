package com.dotcms.util;

import com.dotmarketing.util.Logger;
import java.beans.Statement;
import java.io.Serializable;
import java.lang.reflect.Constructor;

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

                t = clazz.getDeclaredConstructor().newInstance();
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
    }

    /**
     * Java Beans set value utility
     * The method d
     * @param target any object
     * @param method any method that takes one single param it doesnt have to be named setSomething
     * @param value The param expected by the method we intent to fire
     */
    public static void setValue(final Object target, final String method,final Object value) {
        //Uses Java Beans API
        final Statement statement = new Statement(target, method, new Object[] {value});
        try {
            statement.execute();
        } catch (Exception e) {
            Logger.warn(ReflectionUtils.class,String.format("Unable to set value via %s into bean %s",method, target), e);
        }
    }


} // E:O:F:ReflectionUtils.
