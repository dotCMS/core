package com.dotcms.saml.utils;

import com.dotcms.saml.service.internal.MetaDescriptorService;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Just a class to instance without exceptions.
 * 
 * @author jsanca
 */
public class InstanceUtil {

	/**
	 * Creates a new instance avoiding to throw any exception, null in case it
	 * can not be create (if an exception happens). This approach is based on a
	 * constructor with many arguments, keep in mind the method can not find a
	 * constructor to match with the arguments, null will be returned.
	 *
	 * @param className
	 * @param tClass
	 * @param arguments
	 * @param <T>
	 * @return T
	 */
	public static final <T> T newInstance(
			final String className, final Class<T> tClass, final Object... arguments) {

		T t = null;
		Constructor<?> constructor = null;
		Class<?>[] parameterTypes = null;
		Class<T> clazz = tClass;

		if ( StringUtils.isNotBlank(className)) {

			clazz = getClass( className );
		}

		if ( null != clazz ) {

			try {
				parameterTypes = getTypes( arguments );
				constructor = clazz.getDeclaredConstructor( parameterTypes );
				t = (T) constructor.newInstance( arguments );
			} catch (Exception e) {

				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}

		return t;
	}

	/**
	 * Get the types of an array, you can pass an array or a comma separated
	 * arguments.
	 *
	 * @param array
	 *            - {@link Object}
	 * @return array of Class
	 */
	public static final Class<?>[] getTypes(final Object... array) {

		Class<?>[] parameterTypes = null;

		if ( null != array ) {

			parameterTypes = new Class[ array.length ];

			for ( int i = 0; i < array.length; ++i ) {

				parameterTypes[ i ] = array[ i ].getClass();
			}
		}

		return parameterTypes;
	}

	/**
	 * Tries to create a new instance from the className, otherwise creates a
	 * new from tClass. Null if it couldn't at all
	 *
	 * @param className
	 * @param tClass
	 * @param <T>
	 * @return T
	 */
	public static <T> T newInstance(final String className, final Class<T> tClass) {

		T t = null;

		if (StringUtils.isNotBlank(className)) {

			try {

				t = (T) Class.forName( className ).newInstance();
			} catch (final Exception e) {

				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING,
						"Couldn't create from the classname: " + className + ", going to create: " + tClass.getName() );
				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, e.getMessage(), e );

				t = newInstance( tClass );
			}
		} else {

			t = newInstance( tClass );
		}

		return t;
	}

	/**
	 * Tries to create a new instance from the className, otherwise creates a
	 * new from tClass. Null if it couldn't at all
	 *
	 * @param className
	 * @param tClass
	 * @param <T>
	 * @return T
	 */
	public static <T> T newInstance(final String className, final Supplier<T> tClass) {

		T t = null;

		if (StringUtils.isNotBlank(className)) {

			try {

				t = (T) Class.forName( className ).newInstance();
			} catch (final Exception e) {

				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING,
						"Couldn't create from the classname: " + className + ", going to create a default one");
				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, e.getMessage(), e );

				t = tClass.get();
			}
		} else {

			t = tClass.get();
		}

		return t;
	}

	/**
	 * Just get a new instance without throwing an exception. Null if couldn't
	 * create the instance.
	 * 
	 * @param tClass
	 *            {@link Class}
	 * @param <T>
	 * @return T
	 */
	public static <T> T newInstance(final Class<T> tClass) {

		T t = null;

		try {
			t = tClass.newInstance();
		} catch (final Exception e1) {

			Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, "Couldn't create from the class: " + tClass.getName() );
			Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, e1.getMessage(), e1 );
			t = null;
		}

		return t;
	}

	/**
	 * Get a {@link Class} object based on the className, full if the class does
	 * not exists or invalid.
	 * 
	 * @param className
	 *            {@link String}
	 * @return Class
	 */
	public static Class getClass(final String className) {
		Class clazz = null;

		if (StringUtils.isNotBlank(className)){

			try {

				clazz = Class.forName( className );
			} catch ( ClassNotFoundException e ) {

				Logger.getLogger(InstanceUtil.class.getName()).log(Level.WARNING, e.getMessage(), e );
				clazz = null;
			}
		}

		return clazz;
	}

	private static final Map<Class, Object> instanceMap = new ConcurrentHashMap<>();
	public static void putInstance(final Class<?> clazz, final Object instance) {
		instanceMap.put(clazz, instance);
	}

	public static <T> T getInstance(final Class<T> clazz) {
		return (T)instanceMap.get(clazz);
	}
}
