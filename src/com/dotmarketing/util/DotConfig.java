package com.dotmarketing.util;

import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;

public class DotConfig {

	//Generated File Indicator

	//Config internal properties
	private static int refreshInterval = 5; //In minutes, Default 5 can be overridden in the config file as config.refreshinterval int property
	private static Date lastRefreshTime = new Date ();
	private static PropertiesConfiguration props = null;
	private static ServletContext context;

	//Config internal methods
	public static void initializeConfig (ServletContext servletContext) {
		context = servletContext;
	    _loadProperties();
	}

	private static void _loadProperties () {

		String configDirectory = context.getRealPath( File.separator + "WEB-INF");
        String clusterConfig = configDirectory + File.separator + "dotcms-config-cluster.properties";

        File f = new File(clusterConfig);
        Date lastModified = new Date(f.lastModified());

        if (lastModified.after(lastRefreshTime) || props == null) {
        	try {
        		Logger.info(DotConfig.class, "Loading dotCMS Cluster properties...");
        		InputStream propsInputStream = new FileInputStream (f);
        		DotConfig.props = new PropertiesConfiguration ();
        		props.load(new InputStreamReader(propsInputStream));
        		propsInputStream.close();
        		try {
        			int interval = props.getInt("config.refreshinterval");
        			refreshInterval = interval;
        			Logger.info(DotConfig.class, "Assigned custom refresh interval: " + interval + " minutes.");
        		} catch (NoSuchElementException e) {
        			Logger.info(DotConfig.class, "Assigned default refresh interval: " + refreshInterval + " minutes.");
        		}
        		Logger.info(DotConfig.class, "dotCMS Properties Loaded");
        	} catch (Exception e) {
        		Logger.fatal(DotConfig.class, "Exception loading properties", e);
        		props = null;
        	}
        }
        DotConfig.lastRefreshTime = new Date ();
	}

	private static void _refreshProperties () {
	    if(System.currentTimeMillis() > lastRefreshTime.getTime() + (refreshInterval * 60 * 1000) || props == null){
	    	_loadProperties();
	    }
	}
	/**
	 * Returns a string property
	 * @param name The name of the property to locate.
	 * @param defValue Value to return if property is not found.
	 * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each element).
	 */
	public static String getStringProperty(String name,String defValue) {
		_refreshProperties ();
        if ( props == null ) {
            return defValue;
        }
        String[] propsArr = props.getStringArray(name);
	    String property = new String ();
	    int i = 0;
	    if ((propsArr !=null) &&(propsArr.length>0)) {
	    for (String propItem : propsArr) {
	    	if (i > 0)
	    			property += ",";
	            property += propItem;
	            i++;
	        }
	    } else {
	    	property=defValue;
	    }
	    return property;
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
	    return props.getFloat(name);
	}

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

	public static boolean getBooleanProperty (String name, boolean defaultVal) {
	    _refreshProperties ();
        if ( props == null ) {
            return defaultVal;
        }
        return props.getBoolean(name, defaultVal);
	}

	@SuppressWarnings("unchecked")
	public static Iterator<String> getKeys () {
	    _refreshProperties ();
	    return props.getKeys();
	}

	public static boolean containsProperty(String key) {
		return props.containsKey(key);
	}


}