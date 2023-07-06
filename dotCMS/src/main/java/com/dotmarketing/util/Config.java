package com.dotmarketing.util;


import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.FileWatcherAPI;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import javax.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * This class provides access to the system configuration parameters that are set through the
 * {@code dotmarketing-config.properties}, and the {@code dotcms-config-cluster.properties} files.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 */

public class Config {

    //Generated File Indicator
    public static final String GENERATED_FILE = "dotGenerated_";
    public static final String RENDITION_FILE = "dotRendition_";
    public static final AtomicBoolean useWatcherMode = new AtomicBoolean(true);
    public static final AtomicBoolean isWatching = new AtomicBoolean(false);

	private static org.eclipse.microprofile.config.Config config = null;

	public Config() {
	}

    /**
     * If this property is set in the dotmarketing-config, will try to use the interpolator for the
     * properties Otherwise will use {@link SystemEnvironmentConfigurationInterpolator}
     */
    public static final String DOTCMS_CUSTOM_INTERPOLATOR = "dotcms.custom.interpolator";

	/**
	 * If this property is set, defines the way you want to use to monitoring the changes over the dotmarketing-config.properties file.
	 * By default is true and means that the {@link FileWatcherAPI} will be used to monitoring any change over the file and refresh the property based on it.
	 * If you set it to false, will use the legacy mode previously used on dotCMS.
	 */
	public static final String DOTCMS_USEWATCHERMODE = "dotcms.usewatchermode";
	public static final String DYNAMIC_CONTENT_PATH = "DYNAMIC_CONTENT_PATH";
	public static final String DEFAULT_DYNAMIC_CONTENT_PATH = "dotsecure";
	public static final String ASSET_REAL_PATH = "ASSET_REAL_PATH";
	public static final String DEFAULT_ASSET_REAL_PATH = "DEFAULT_ASSET_REAL_PATH";
	public static int DB_VERSION=0;


    //Object Config properties
	public static javax.servlet.ServletContext CONTEXT = null;
	public static String CONTEXT_PATH = null;



	//Config internal properties
	private static int refreshInterval = 5; //In minutes, Default 5 can be overridden in the config file as config.refreshinterval int property
	private static Date lastRefreshTime = new Date ();
;
	private static ClassLoader classLoader = null;
    protected static URL dotmarketingPropertiesUrl = null;
    protected static URL clusterPropertiesUrl = null;
    private static int prevInterval = Integer.MIN_VALUE;
    private static FileWatcherAPI fileWatcherAPI = null;

	private static RuntimeConfigWriter memoryConfigWriter;

	/**
	 * Config internal methods
	 */
	public static void initializeConfig () {
		config = ConfigProvider.getConfig();
		memoryConfigWriter = CDI.current().select(RuntimeConfigWriter.class).get();
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

        final String[] propsArr = getStringArrayProperty(name,
                defValue == null ? null : new String[]{defValue});

        if (propsArr == null || propsArr.length == 0) {
            return defValue;
        }

        return String.join(",", propsArr);

    }

	public static <V> V getProperty(final String name, final V defValue, Class<V> clazz) {
		return config.getOptionalValue(name, clazz).orElse(defValue);
	}


	/**
	 * Returns return Path stored in a property, if the property is not found, it will return the default value
	 * Optionally try to create if the folder does not exist
	 *
	 * @param name     The name of the property to locate.
	 * @param defValue String value of the path to use if property is not found.
	 * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
	 * element).
	 */
	public static Path getPathProperty(final String name, final String defValue, boolean create) {

		String stringVal = getStringProperty(name, defValue);
		Path path = Paths.get(stringVal);
		if (create && !Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				throw new DotRuntimeException("Unable to create directory for property: " +name +" with path "+ path,e);
			}
		}
		return path;
	}

	public static Path getDynamicContentPath() {
		return getPathProperty(DYNAMIC_CONTENT_PATH, DEFAULT_DYNAMIC_CONTENT_PATH, true);
	}

	public static Path getVelocityRoot() {
		Path baseRoot = getPathProperty("VELOCITY_ROOT", "${CONTEXT_ROOT}/WEB-INF/velocity", false);
		return baseRoot;
	}

	public static Path getAssetRealPath() {
		return Paths.get(ConfigUtils.getAbsoluteAssetsRootPath());
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

		return config.getOptionalValue(name, String.class).orElse(null);
        
    }


    public static String getStringProperty (String name) {
		return config.getOptionalValue(name, String.class).orElse(nullDefault(name));
	}

	public static String getProperty(String name) {
		return config.getOptionalValue(name, String.class).orElse(nullDefault(name));
	}

	private static String nullDefault(String name) {
		if (config.getOptionalValue("throwExceptionOnNullConfigValue", Boolean.class).orElse(false)) {
			throw new IllegalArgumentException("Property is required");
		} else if (config.getOptionalValue("warnOnNullConfigValue", Boolean.class).orElse(false)) {
			Logger.warn(Config.class, "Use Optional to set a default value if Config value is not required: " + name);
			return null;
		}
		return null;
	}

	public static <V> V getProperty(String name,Class<V> clazz) {
		return config.getOptionalValue(name, clazz).orElseThrow(() -> new IllegalArgumentException("Property " + name + " is required"));
	}
	/**
	 *
	 * @param name
	 * @return
	 */
	public static String[] getStringArrayProperty (String name) {
	    return config.getOptionalValue(name, String[].class).orElse(null);
	}

    /**
     * Transform an array into an array of entity, needs a transformer to convert the string from
     * the config to object and the class. In addition if the name does not exists, the supplier
     * will be invoke
     *
     * @param name                      {@link String} name of the array property
     * @param stringToEntityTransformer {@link StringToEntityTransformer} transformer to string to
     *                                  T
     * @param clazz                     {@link Class}
     * @param defaultSupplier           {@link Supplier}
     * @param <T>
     * @return Array of T
     */
    public static <T> T[] getCustomArrayProperty(final String name,
            final StringToEntityTransformer<T> stringToEntityTransformer,
            final Class<T> clazz,
            final Supplier<T[]> defaultSupplier) {

		return config.getOptionalValue(name, String[].class).map( v-> convert(v,clazz,stringToEntityTransformer)).orElseGet(
				defaultSupplier::get);
	}

    private static <T> T[] convert(final String[] values, final Class<T> clazz,
            final StringToEntityTransformer<T> stringToEntityTransformer) {

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
		return config.getOptionalValue(name, String[].class).orElse(defaultValue);
    }
	/**
	 * @deprecated  Use getIntProperty(String name, int default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static int getIntProperty (final String name) {
	    return config.getOptionalValue(name, Integer.class).orElse(0);
	}

	public static long getLongProperty (final String name, final long defaultVal) {
		return config.getOptionalValue(name, Long.class).orElse(defaultVal);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static int getIntProperty (final String name, final int defaultVal) {
		return config.getOptionalValue(name, Integer.class).orElse(defaultVal);
	}

	/**
	 * @deprecated  Use getFloatProperty(String name, float default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static float getFloatProperty (final String name) {
		return config.getOptionalValue(name, float.class).orElse(0f);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static float getFloatProperty (final String name, final float defaultVal) {
		return config.getOptionalValue(name, Float.class).orElse(defaultVal);
	}

	/**
	 * @deprecated  Use getBooleanProperty(String name, boolean default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static boolean getBooleanProperty (String name) {
		return config.getOptionalValue(name, Boolean.class).orElse(false);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static boolean getBooleanProperty (String name, boolean defaultVal) {
		return config.getOptionalValue(name, Boolean.class).orElse(defaultVal);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public static void setProperty(String key, Object value) {
		memoryConfigWriter.setOverride(key, Objects.toString(value,null));
	}


	public static void removeProperty(String key) {
		memoryConfigWriter.removeOverride(key);
	}


	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Iterator<String> getKeys () {
	   return config.getPropertyNames().iterator();
	}

	/**
	 * 
	 * @param prefix
	 * @return
	 */
	@SuppressWarnings ( "unchecked" )
	public static Iterator<String> subset ( String prefix ) {
		return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
				.filter(key -> key.startsWith(prefix+"."))
				.map(key -> key.substring(prefix.length()+1))
				.iterator();
	}


	public static RuntimeConfigWriter getConfigWriter(){
		return memoryConfigWriter;
	}

    /**
     * Spindle Config
     *
     * @param myApp
     */
    public static void setMyApp(javax.servlet.ServletContext myApp) {
        CONTEXT = myApp;
        CONTEXT_PATH = myApp.getRealPath("/");
    }


    /**
     *
     */
    public static void forceRefresh() {
        lastRefreshTime = new Date(0);
    }




}
