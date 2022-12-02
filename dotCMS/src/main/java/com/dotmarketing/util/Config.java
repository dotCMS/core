package com.dotmarketing.util;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import javax.validation.constraints.NotNull;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.ConfigurationInterpolator;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;

/**
 * This class provides access to the system configuration parameters that are
 * set through the {@code dotmarketing-config.properties}, and the
 * {@code dotcms-config-cluster.properties} files.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */

public class Config {

    @VisibleForTesting
    protected Config(boolean testing) {
        setTestingMode(testing);
    }
    
    
    
    private Config() {
        throw new DotRuntimeException("Config is a Util class");
    }
    
    
	//Generated File Indicator
	public static final String GENERATED_FILE ="dotGenerated_";
	public static final String RENDITION_FILE ="dotRendition_";


	/**
	 * If this property is set in the dotmarketing-config, will try to use the interpolator for the properties
	 * Otherwise will use {@link SystemEnvironmentConfigurationInterpolator}
	 */
	public static final String DOTCMS_CUSTOM_INTERPOLATOR = "dotcms.custom.interpolator";


	public static int DB_VERSION=0;

    //Object Config properties      n
	public static javax.servlet.ServletContext CONTEXT = null;
	public static String CONTEXT_PATH = null;



	//Config internal properties
	private static final PropertiesConfiguration props = new PropertiesConfiguration();
	@VisibleForTesting
    protected static String[] propertyFiles = new String[] {"dotmarketing-config.properties","dotcms-config-cluster.properties"};

    
    
    private static String[] getPropertyFiles() {
        
        String envValue = System.getenv("DOT_CONFIG_PROPERTY_FILES");
        if(envValue!=null) {
            return envValue.split("\\s*,\\s*");
        }
        return propertyFiles;
        
        
    }
    
    
    private static final AtomicReference<Boolean> testingModeLazy = new AtomicReference<>();

    public static void setTestingMode(boolean testingMode) {
        testingModeLazy.compareAndSet(null, testingMode);
    }
    /**
     * Checks if we are in testing mode
     * @param needsTestingMode
     * @return
     */
    private static void needTestingMode() {

        testingModeLazy.compareAndSet(null, true);

        if(!testingModeLazy.get()) {
            Logger.info(Config.class, "not in testing mode, unable to reload props");
            throw new DotRuntimeException("not in testing mode, unable to reload props");
        }
    }
    
    public static void reloadProps() {
        needTestingMode();
        loadProperties();
    }
    
    static {
        loadProperties();
    }
    
    
    /**
     * 
     */
    private static synchronized void loadProperties() {
        props.clear();

        for (String fileName : getPropertyFiles()) {
            try (InputStream in = Config.class.getResourceAsStream("/" + fileName)) {
                Logger.info(Config.class, "Loading dotCMS Properties [" + fileName + "] ");
                props.load(in);
            } catch (Exception e) {
                Logger.warn(Config.class, "Unable to load [" + fileName + "] ", e);
                throw new DotRuntimeException(e);
            }
        }

        readEnvironmentVariables();
        postProperties();
        
    }





	/**
	 * Does the post process properties based on the interpolator
	 */
    protected static void postProperties () {

        final String customConfigurationInterpolator = props.getString(DOTCMS_CUSTOM_INTERPOLATOR, System.getenv(envKey(DOTCMS_CUSTOM_INTERPOLATOR)));
        final ConfigurationInterpolator interpolator = UtilMethods.isSet(customConfigurationInterpolator)
            ? (ConfigurationInterpolator) ReflectionUtils.newInstance(customConfigurationInterpolator)
            : SystemEnvironmentConfigurationInterpolator.INSTANCE;

        final Configuration configuration = interpolator.interpolate(props);

		if(configuration instanceof PropertiesConfiguration) {
		    PropertiesConfiguration propConfig  = (PropertiesConfiguration)configuration;
		    propConfig.getKeys().forEachRemaining(k->
		        props.setProperty(k, propConfig.getProperty(k))
		        
		    );
		}
	}



	private static final String ENV_PREFIX="DOT_";

	private static void readEnvironmentVariables() {
		synchronized (Config.class) {
			System.getenv().entrySet().stream().filter(e -> e.getKey().startsWith(ENV_PREFIX))
					.forEach(e -> props.setProperty(e.getKey(), e.getValue()));
		}
	}
	
	
	private static String envKey(final String theKey) {

        String envKey = ENV_PREFIX + theKey.toUpperCase().replace(".", "_");
        while (envKey.contains("__")) {
            envKey = envKey.replace("__", "_");
        }
        return envKey.endsWith("_") ? envKey.substring(0, envKey.length() - 1) : envKey;

	}

	/**
	 * Given a property name, evaluates if it belongs to the environment variables.
	 *
	 * @param key property name
	 * @return true if key is found when looked by its environment variable name equivalent and if it has properties
	 * associated to, otherwise false.
	 */
	public static boolean isKeyEnvBased(final String key) {
		final String envKey = envKey(key);

		if (!props.containsKey(envKey)) {
			return false;
		}

		final String[] properties = props.getStringArray(envKey);
		return properties != null && properties.length > 0;
	}
	
	/**
	 * Returns a string property
	 *
	 * @param name     The name of the property to locate.
	 * @param defValue Value to return if property is not found.
	 * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
	 * element).
	 */
	public static String getStringProperty(final String name, final String defValue) {

	    final String[] propsArr = getStringArrayProperty(name,  defValue==null ? null : new String[] {defValue});

		if (propsArr == null || propsArr.length == 0) {
		    return defValue;
		} 
		
		return String.join(",", propsArr);
		
	}

	/**
	 * this is only here so the old tests pass
	 * 
	 * @param name
	 * @param defValue
	 * @param thing
	 * @return
	 */
	@VisibleForTesting
	@Deprecated
    public static String getStringProperty(final String name, final String defValue, boolean thing) {

        return getStringProperty(name, defValue);
        
    }

	
	
	

	/**
	 * @deprecated  Use getStringProperty(String name, String default) and
	 * set an intelligent default
	 */
	@Deprecated
    public static String getStringProperty (String name) {
        return getStringProperty(name, null);
    }

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static String[] getStringArrayProperty (String name) {
	    return getStringArrayProperty(name, null);
	}

	/**
	 * Transform an array into an array of entity, needs a transformer to convert the string from the config to object
	 * and the class.
	 * In addition if the name does not exists, the supplier will be invoke
	 * @param name {@link String} name of the array property
	 * @param stringToEntityTransformer {@link StringToEntityTransformer} transformer to string to T
	 * @param clazz {@link Class}
	 * @param defaultSupplier {@link Supplier}
	 * @param <T>
	 * @return Array of T
	 */
	public static <T>  T[] getCustomArrayProperty(@NotNull final String name,
	                @NotNull final StringToEntityTransformer<T> stringToEntityTransformer,
												  @NotNull final Class<T> clazz,
												  @NotNull final Supplier<T[]> defaultSupplier) {

		final String [] values = getStringArrayProperty(name, new String[0]);
		return props.containsKey(name)?convert(values, clazz, stringToEntityTransformer): defaultSupplier.get();
	}

	private static <T> T[] convert(@NotNull final String[] values, @NotNull final Class<T> clazz, @NotNull final StringToEntityTransformer<T> stringToEntityTransformer) {

		final T[] entities = (T[]) Array.newInstance(clazz, values.length);

		for (int i = 0; i < values.length; ++i) {

			entities[i] = stringToEntityTransformer.from(values[i]);
		}

		return entities;
	}

    /**
     * If config value == null, returns the default
     * 
     * @param name
     * @param defaultValue
     * @return
     */
    public static String[] getStringArrayProperty(final String name, final String[] defaultValue) {

        if (props.containsKey(envKey(name))) {
            return props.getStringArray(envKey(name));
        }
        if (props.containsKey(name)) {
            return props.getStringArray(name);
        }
        return defaultValue;
    }
	/**
	 * @deprecated  Use getIntProperty(String name, int default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static int getIntProperty (final String name) {    

        Integer value = Try.of(()->props.getInt(envKey(name))).getOrNull();
        if(value!=null) {
            return value;
        }
	    
	    
	    return props.getInt(name);
	}

	public static long getLongProperty (final String name, final long defaultVal) {

		Long value = Try.of(()->props.getLong(envKey(name))).getOrNull();
		if ( value != null ) {
			return value;
		}
		return props.getLong(name, defaultVal);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static int getIntProperty (final String name, final int defaultVal) {

        Integer value = Try.of(()->props.getInt(envKey(name))).getOrNull();
        if(value!=null) {
            return value;
        }
        
        return props.getInt(name, defaultVal);
	}

	/**
	 * @deprecated  Use getFloatProperty(String name, float default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static float getFloatProperty (final String name) {

        Float value = Try.of(()->props.getFloat(envKey(name))).getOrNull();
        if(value!=null) {
            return value;
        }
        
	    
	    return props.getFloat( name );
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static float getFloatProperty (final String name, final float defaultVal) {

        Float value = Try.of(()->props.getFloat(envKey(name))).getOrNull();
        if(value!=null) {
            return value;
        }
        return props.getFloat(name, defaultVal);
	}

	/**
	 * @deprecated  Use getBooleanProperty(String name, boolean default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static boolean getBooleanProperty (String name) {

        Boolean value = Try.of(()->props.getBoolean(envKey(name))).getOrNull();
        if(value!=null) {
            return value;
        }
        return props.getBoolean(name);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static boolean getBooleanProperty (String name, boolean defaultVal) {

        final Boolean value = props.containsKey(envKey(name))  ? Try.of(()->props.getBoolean(envKey(name))).getOrNull() : null;
        if(null != value) {
            return value;
        }
        return props.getBoolean(name, defaultVal);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public static void setProperty(String key, Object value) {
	    needTestingMode();

		props.setProperty(key, value);
		
	}

	
    /**
     * 
     * @param key
     * @param value
     */
    public static void addProperty(String key, Object value) {
        needTestingMode();
        props.addProperty(key, value);
        
    }
	
	
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Iterator<String> getKeys () {

	    return ImmutableSet.copyOf(props.getKeys()).iterator();
	}

	/**
	 * 
	 * @param prefix
	 * @return
	 */
	@SuppressWarnings ( "unchecked" )
	public static Iterator<String> subset ( String prefix ) {

		return ImmutableSet.copyOf(props.subset(prefix).getKeys()).iterator();
	}


	/**
	 * Spindle Config
	 * 
	 * @param myApp
	 */
	public static void setMyApp(javax.servlet.ServletContext myApp) {
	    if(CONTEXT!=null) {
	        System.err.println("CONTEXT is already set");
	        return;
	    }
		CONTEXT = myApp;
		CONTEXT_PATH = myApp.getRealPath("/");
	}


}
