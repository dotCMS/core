package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.repackage.com.google.common.io.Files;
import com.dotcms.repackage.org.apache.commons.configuration.PropertiesConfiguration;
import com.dotmarketing.db.DbConnectionFactory;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;

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

	private static final String BLANK = "";

	//Generated File Indicator
	public static final String GENERATED_FILE ="dotGenerated_";
	public static final String RENDITION_FILE ="dotRendition_";
	public static int DB_VERSION=0;

    //Object Config properties
	public static javax.servlet.ServletContext CONTEXT = null;
	public static String CONTEXT_PATH = null;

	//PERMISSION CONSTANTS
	public static final int PERMISSION_READ = 1;
	public static final int PERMISSION_WRITE = 2;
	public static final int PERMISSION_PUBLISH = 4;

	//Config internal properties
	private static int refreshInterval = 5; //In minutes, Default 5 can be overridden in the config file as config.refreshinterval int property
	private static Date lastRefreshTime = new Date ();
	private static PropertiesConfiguration props = null;
	private static ClassLoader classLoader = null;
    protected static URL dotmarketingPropertiesUrl = null;
    protected static URL clusterPropertiesUrl = null;
    private static int prevInterval = Integer.MIN_VALUE;

    private static final String syncMe = "esSync";

	/**
	 * Config internal methods
	 */
	public static void initializeConfig () {
	    classLoader = Thread.currentThread().getContextClassLoader();
	    _loadProperties();
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

        try {

            Logger.info( Config.class, "Loading dotCMS [" + fileName + "] Properties..." );

            if ( props == null ) {
                props = new PropertiesConfiguration();
            }

            InputStream propsInputStream = new FileInputStream( fileToRead );
            props.load( new InputStreamReader( propsInputStream ) );
            propsInputStream.close();

            Logger.info( Config.class, "dotCMS Properties [" + fileName + "] Loaded" );
        } catch ( Exception e ) {
            Logger.fatal( Config.class, "Exception loading properties for file [" + fileName + "]", e );
            props = null;
        }
    }

    /**
     * 
     */
	private static void _refreshProperties () {
	    if(System.currentTimeMillis() > lastRefreshTime.getTime() + (refreshInterval * 60 * 1000) || props == null){
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
		String result = BLANK;
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
	    return props.getKeys();
	}

	/**
	 * 
	 * @param prefix
	 * @return
	 */
	@SuppressWarnings ( "unchecked" )
	public static Iterator<String> subset ( String prefix ) {
		_refreshProperties();
		return props.subset(prefix).getKeys();
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
