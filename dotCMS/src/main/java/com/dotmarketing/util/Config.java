package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.util.ConfigurationInterpolator;
import com.dotcms.util.FileWatcherAPI;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

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

	//Generated File Indicator
	public static final String GENERATED_FILE ="dotGenerated_";
	public static final String RENDITION_FILE ="dotRendition_";
	public static final AtomicBoolean useWatcherMode = new AtomicBoolean(true);
	public static final AtomicBoolean isWatching     = new AtomicBoolean(false);


	/**
	 * If this property is set in the dotmarketing-config, will try to use the interpolator for the properties
	 * Otherwise will use {@link SystemEnvironmentConfigurationInterpolator}
	 */
	public static final String DOTCMS_CUSTOM_INTERPOLATOR = "dotcms.custom.interpolator";

	/**
	 * If this property is set, defines the way you want to use to monitoring the changes over the dotmarketing-config.properties file.
	 * By default is true and means that the {@link FileWatcherAPI} will be used to monitoring any change over the file and refresh the property based on it.
	 * If you set it to false, will use the legacy mode previously used on dotCMS.
	 */
	public static final String DOTCMS_USEWATCHERMODE = "dotcms.usewatchermode";
	public static int DB_VERSION=0;

    //Object Config properties      n
	public static javax.servlet.ServletContext CONTEXT = null;
	public static String CONTEXT_PATH = null;

	//PERMISSION CONSTANTS
	public static final int PERMISSION_READ = 1;
	public static final int PERMISSION_WRITE = 2;
	public static final int PERMISSION_PUBLISH = 4;

	//Config internal properties
	private static int refreshInterval = 5; //In minutes, Default 5 can be overridden in the config file as config.refreshinterval int property
	private static Date lastRefreshTime = new Date ();
	protected static PropertiesConfiguration props = null;
	private static ClassLoader classLoader = null;
    protected static URL dotmarketingPropertiesUrl = null;
    protected static URL clusterPropertiesUrl = null;
    private static int prevInterval = Integer.MIN_VALUE;
    private static FileWatcherAPI fileWatcherAPI = null;

    private static final String syncMe = "esSync";

	/**
	 * Config internal methods
	 */
	public static void initializeConfig () {
	    classLoader = Thread.currentThread().getContextClassLoader();
	    _loadProperties();
	}

	private static void registerWatcher(final File fileToRead) {

		initWatcherAPI();
		if (null != fileWatcherAPI) {

			// if we are not already watching, so register the waticher
			if (!isWatching.get()) {
				try {

					Logger.debug(APILocator.class, "Start watching: " + fileToRead);
					fileWatcherAPI.watchFile(fileToRead, () -> _loadProperties());
					isWatching.set(true);
				} catch (IOException e) {
					Logger.error(Config.class, e.getMessage(), e);
				}
			}
		} else {
			// if not fileWatcherAPI could not monitoring, so use the fallback
			useWatcherMode.set(false); isWatching.set(false);
		}
	} // registerWatcher.

	private static void initWatcherAPI() {

		// checki if the watcher is already instantiated.
		if (null == fileWatcherAPI) {
			synchronized (syncMe) {

				if (null == fileWatcherAPI) {

					fileWatcherAPI = APILocator.getFileWatcherAPI();
				}
			}
		}
	}

	private static void unregisterWatcher(final File fileToRead) {

		initWatcherAPI();
		if (null != fileWatcherAPI) {

			Logger.debug(APILocator.class, "Stop watching: " + fileToRead);
			fileWatcherAPI.stopWatchingFile(fileToRead);
		}
	}
	/**
	 * 
	 */
    private static void _loadProperties () {

         if ( classLoader == null ) {
            classLoader = Thread.currentThread().getContextClassLoader();
            Logger.info(Config.class, "Initializing properties reader.");
        }

        //dotmarketing config file
        String propertyFile = "dotmarketing-config.properties";
        if ( dotmarketingPropertiesUrl == null ) {
            dotmarketingPropertiesUrl = classLoader.getResource( propertyFile );
        }

        //cluster config file
        propertyFile = "dotcms-config-cluster.properties";
        if ( clusterPropertiesUrl == null ) {
            clusterPropertiesUrl = classLoader.getResource( propertyFile );
        }

        //Reading both property files
        readProperties( dotmarketingPropertiesUrl, clusterPropertiesUrl );
    }

	/**
	 * Reads the properties on the dotmarketing-config.properties and the
	 * dotcms-config-cluster.properties properties files.
	 *
	 * @param dotmarketingURL
	 * @param clusterURL
	 */
	private static void readProperties(URL dotmarketingURL, URL clusterURL) {
		File dotmarketingFile = new File(dotmarketingURL.getPath());
		Date lastDotmarketingModified = new Date(
				dotmarketingFile.lastModified());
		File clusterFile = new File(clusterURL.getPath());
		Date lastClusterModified = new Date(clusterFile.lastModified());

		if (props == null) {
			synchronized (syncMe) {
				if (props == null) {
					readProperties(dotmarketingFile,
							"dotmarketing-config.properties");
					readProperties(clusterFile,
							"dotcms-config-cluster.properties");
				}
			}
		} else {
			// Refresh the properties if changes detected in any of these
			// properties files
			if (lastDotmarketingModified.after(lastRefreshTime)
					|| lastClusterModified.after(lastRefreshTime)) {
				synchronized (syncMe) {
					if (lastDotmarketingModified.after(lastRefreshTime)
							|| lastClusterModified.after(lastRefreshTime)) {
						try {
							props = new PropertiesConfiguration();
							// Cleanup and read the properties for both files
							readProperties(dotmarketingFile,
									"dotmarketing-config.properties");
							readProperties(clusterFile,
									"dotcms-config-cluster.properties");
						} catch (Exception e) {
							Logger.fatal(
									Config.class,
									"Exception loading property files [dotmarketing-config.properties, dotcms-config-cluster.properties]",
									e);
							props = null;
						}
					}
				}
			}
		}
		String type = "";
		try {
			refreshInterval = props.getInt("config.refreshinterval");
			type = "custom";
		} catch (NoSuchElementException e) {
			// Property not present, use default interval value
			type = "default";
		} finally {
			// Display log message the first time, and then only if interval changes
			if (prevInterval != refreshInterval) {
				Logger.info(Config.class, "Assigned " + type + " refresh: "
						+ refreshInterval + " minutes.");
				prevInterval = refreshInterval;
			}
		}
		// Set the last time we refresh/read the properties files
		Config.lastRefreshTime = new Date();
	}

    /**
     * Reads a given property file and appends its content to the current read properties
     *
     * @param fileToRead
     * @param fileName
     */
    private static void readProperties ( File fileToRead, String fileName ) {

		InputStream propsInputStream = null;

        try {

            Logger.info( Config.class, "Loading dotCMS [" + fileName + "] Properties..." );

            if ( props == null ) {
                props = new PropertiesConfiguration();
            }

            propsInputStream = Files.newInputStream(fileToRead.toPath());
            props.load( new InputStreamReader( propsInputStream ) );
            Logger.info( Config.class, "dotCMS Properties [" + fileName + "] Loaded" );
            postProperties();
            // check if the configuration for the watcher has changed.
			useWatcherMode.set(getBooleanProperty(DOTCMS_USEWATCHERMODE, true));
			if (useWatcherMode.get()) {

				registerWatcher (fileToRead);
			} else if (isWatching.get()) {
				unregisterWatcher (fileToRead);
				isWatching.set(false);
			}
        } catch ( Exception e ) {
            Logger.fatal( Config.class, "Exception loading properties for file [" + fileName + "]", e );
            props = null;
        } finally {

			IOUtils.closeQuietly(propsInputStream);
		}
	}



	/**
	 * Does the post process properties based on the interpolator
	 */
    protected static void postProperties () {

    	final String customConfigurationInterpolator       = getStringProperty(DOTCMS_CUSTOM_INTERPOLATOR, null);
    	final ConfigurationInterpolator customInterpolator = UtilMethods.isSet(customConfigurationInterpolator)?
				(ConfigurationInterpolator) ReflectionUtils.newInstance(customConfigurationInterpolator) :null;
		final ConfigurationInterpolator interpolator       = (null != customInterpolator)?
				customInterpolator:SystemEnvironmentConfigurationInterpolator.INSTANCE;
		final Configuration configuration = interpolator.interpolate(props);

		props = (configuration instanceof PropertiesConfiguration)?(PropertiesConfiguration)configuration: props;
	}

    /**
     * 
     */
	private static void _refreshProperties () {

		if ((props == null) || // if props is null go ahead.
				(
						(!useWatcherMode.get()) && // if we are using watcher mode, do not need to check this
						(System.currentTimeMillis() > lastRefreshTime.getTime() + (refreshInterval * 60 * 1000))
				)) {
			_loadProperties();
		}
	}

	/**
	 * 
	 * @param name
	 * @param defValue
	 * @return
	 */
	public static String getStringProperty(String name, String defValue) {
		return getStringProperty(name, defValue, true);
	}

	/**
	 * Returns a string property
	 *
	 * @param name     The name of the property to locate.
	 * @param defValue Value to return if property is not found.
	 * @param forceDefaultToString If the provided default value should be returned as a string, even when null (marshals literal null to "null").
	 * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
	 * element).
	 */
	public static String getStringProperty(String name, String defValue, boolean forceDefaultToString) {
		_refreshProperties();
		String result = defValue;

		if(props != null) {
			String[] propsArr = props.getStringArray(name);
			StringBuilder property = new StringBuilder();

			if(propsArr != null && propsArr.length > 0) {
				buildProperty(propsArr, property);
				result = property.toString();
			} else if(forceDefaultToString) {
				result = String.valueOf(defValue);
			}
		} else {
			// default is not forced to string here for historical reasons. Presumably props is never actually null.
			result = defValue;
		}
		return result;
	}

	/**
	 * Returns a string property. The {@link Supplier} is useful when you want
	 * lazy evaluation for the default value. This means that figuring out the
	 * value of the supplier will not happen until the logic determines that the
	 * property specified by the {@code name} parameter does not map to a value
	 * in the properties files.
	 *
	 * @param name
	 *            - The name of the property to read.
	 * @param defValue
	 *            - The default value as a {@link Supplier}, in case the
	 *            property is not defined.
	 * @return The value of the specified property, or its default value.
	 */
	public static String getAsString(String name, Supplier<String> defValue) {
		return getAsString(name, defValue, true);
	}

	/**
	 * Returns a string property. The {@link Supplier} is useful when you want
	 * lazy evaluation for the default value. This means that figuring out the
	 * value of the supplier will not happen until the logic determines that the
	 * property specified by the {@code name} parameter does not map to a value
	 * in the properties files.
	 *
	 * @param name
	 *            - The name of the property to read.
	 * @param defValue
	 *            - The default value as a {@link Supplier}, in case the
	 *            property is not defined.
	 * @param forceDefaultToString
	 *            - If the default value is to be returned when the property is
	 *            not defined in the configuration files, set to {@code true}.
	 *            Otherwise, set to {@code false}.
	 * @return The value of the specified property, or its default value.
	 */
	public static String getAsString(String name, Supplier<String> defValue, boolean forceDefaultToString) {
		_refreshProperties();
		String result = StringUtils.EMPTY;
		if (props != null) {
			String[] propsArr = props.getStringArray(name);
			StringBuilder property = new StringBuilder();
			if (propsArr != null && propsArr.length > 0) {
				buildProperty(propsArr, property);
				result = property.toString();
			} else if (forceDefaultToString) {
				result = (defValue != null) ? defValue.get() : "";
			}
		} else {
			// Default is not forced to string here for historical reasons.
			// Presumably props are never actually null.
			result = (defValue != null) ? defValue.get() : "";
		}
		return result;
	}

	/**
	 * 
	 * @param propsArr
	 * @param property
	 */
	private static void buildProperty(String[] propsArr, StringBuilder property) {

		int i = 0;

		for (String propItem : propsArr) {
            if(i > 0) {
                property.append(",");
            }
            property.append(propItem);
            i++;
        }
	}

	/**
	 * @deprecated  Use getStringProperty(String name, String default) and
	 * set an intelligent default
	 */
	@Deprecated
    public static String getStringProperty (String name) {
        _refreshProperties ();
        String[] propsArr = props.getStringArray(name);
        String property = new String ();
        int i = 0;
        for (String propItem : propsArr) {
            if (i > 0)
                property += ",";
            property += propItem;
            i++;
        }
        return property;
    }

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static String[] getStringArrayProperty (String name) {
	    _refreshProperties ();
	    return props.getStringArray(name);
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
	public static <T>  T[] getCustomArrayProperty(final String name,
												  final StringToEntityTransformer<T> stringToEntityTransformer,
												  final Class<T> clazz,
												  final Supplier<T[]> defaultSupplier) {

		final String [] values = getStringArrayProperty(name);
		return props.containsKey(name)?convert(values, clazz, stringToEntityTransformer): defaultSupplier.get();
	}

	private static <T> T[] convert(final String[] values, final Class<T> clazz, final StringToEntityTransformer<T> stringToEntityTransformer) {

		final T[] entities = (T[]) Array.newInstance(clazz, values.length);

		for (int i = 0; i < values.length; ++i) {

			entities[i] = stringToEntityTransformer.from(values[i]);
		}

		return entities;
	}

	/**
	 * If config value == null, returns the default
	 * @param name
	 * @param defaultValue
	 * @return
	 */
  public static String[] getStringArrayProperty (String name, String[] defaultValue) {
      _refreshProperties ();
      
      return props.containsKey(name) ? props.getStringArray(name) : defaultValue;
  }
	/**
	 * @deprecated  Use getIntProperty(String name, int default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static int getIntProperty (String name) {
	    _refreshProperties ();
	    return props.getInt(name);
	}

	public static long getLongProperty (String name, final long defaultVal) {
		_refreshProperties ();
		if ( props == null ) {
			return defaultVal;
		}
		return props.getLong(name, defaultVal);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static int getIntProperty (String name, int defaultVal) {
	    _refreshProperties ();
        if ( props == null ) {
            return defaultVal;
        }
        return props.getInt(name, defaultVal);
	}

	/**
	 * @deprecated  Use getFloatProperty(String name, float default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static float getFloatProperty (String name) {
	    _refreshProperties ();
	    return props.getFloat( name );
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static float getFloatProperty (String name, float defaultVal) {
	    _refreshProperties ();
        if ( props == null ) {
            return defaultVal;
        }
        return props.getFloat(name, defaultVal);
	}

	/**
	 * @deprecated  Use getBooleanProperty(String name, boolean default) and
	 * set an intelligent default
	 */
	@Deprecated
	public static boolean getBooleanProperty (String name) {
	    _refreshProperties ();
	    return props.getBoolean(name);
	}

	/**
	 * 
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static boolean getBooleanProperty (String name, boolean defaultVal) {
	    _refreshProperties ();
        if ( props == null ) {
            return defaultVal;
        }
        return props.getBoolean(name, defaultVal);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public static void setProperty(String key, Object value) {
		if(props!=null) {
			props.setProperty(key, value);
		}
	}

	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Iterator<String> getKeys () {
	    _refreshProperties ();
	    return ImmutableSet.copyOf(props.getKeys()).iterator();
	}

	/**
	 * 
	 * @param prefix
	 * @return
	 */
	@SuppressWarnings ( "unchecked" )
	public static Iterator<String> subset ( String prefix ) {
		_refreshProperties();
		return ImmutableSet.copyOf(props.subset(prefix).getKeys()).iterator();
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public static boolean containsProperty(String key) {
		return props.containsKey(key);
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
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static String getLimitOffsetQuery(int limit, int offset) {
		String db = DbConnectionFactory.getDBType();

	    if (db.equals("PostgreSQL")){
			return " limit " + limit + " offset " + offset;
		}
		else if (db.equals("MySQL")){
			return " limit " + offset + " ," + limit;
		}
		return "";
	}

	/**
	 * 
	 */
	public static void forceRefresh(){
		lastRefreshTime = new Date(0);
	}

}
