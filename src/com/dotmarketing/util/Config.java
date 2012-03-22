package com.dotmarketing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.dotmarketing.db.DbConnectionFactory;

public class Config {
    
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
	private static URL url = null;
	
	//Config internal methods 
	public static void initializeConfig () {
	    classLoader = Thread.currentThread().getContextClassLoader();
	    _loadProperties();
	}
	
	private static void _loadProperties () {
	    if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
		    Logger.info(Config.class, "Initializing properties reader.");
	    }
	    if (url == null)
	        url = classLoader.getResource("dotmarketing-config.properties");
		
		if (url != null) {
		    File f = new File(url.getPath());
		    Date lastModified = new Date(f.lastModified());
		    if (lastModified.after(lastRefreshTime) || props == null) {
		        try {
	                Logger.info(Config.class, "Loading dotCMS Properties...");
		            InputStream propsInputStream = new FileInputStream (f);
		            Config.props = new PropertiesConfiguration ();
		            props.load(new InputStreamReader(propsInputStream));
		            propsInputStream.close();
		            try {
		                int interval = props.getInt("config.refreshinterval");
		                refreshInterval = interval;
		                Logger.info(Config.class, "Assigned custom refresh interval: " + interval + " minutes.");
		            } catch (NoSuchElementException e) {    
		                Logger.info(Config.class, "Assigned default refresh interval: " + refreshInterval + " minutes.");
		            }
	                Logger.info(Config.class, "dotCMS Properties Loaded");
		        } catch (Exception e) {
	                Logger.fatal(Config.class, "Exception loading properties", e);
		            props = null;
		        }
		    }
	        Config.lastRefreshTime = new Date ();
		} else {
            Logger.fatal(Config.class, "DotCMS Properties file (dotmarketing-config.properties) not found.");
		}
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
	
	//Config get and set properties methods
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

	public static int getIntProperty (String name) {
	    _refreshProperties ();
	    return props.getInt(name);
	}

	public static int getIntProperty (String name, int defaultVal) {
	    _refreshProperties ();
	    return props.getInt(name, defaultVal);
	}

	public static float getFloatProperty (String name) {
	    _refreshProperties ();
	    return props.getFloat(name);
	}

	public static float getFloatProperty (String name, float defaultVal) {
	    _refreshProperties ();
	    return props.getFloat(name, defaultVal);
	}

	public static boolean getBooleanProperty (String name) {
	    _refreshProperties ();
	    return props.getBoolean(name);
	}

	public static boolean getBooleanProperty (String name, boolean defaultVal) {
	    _refreshProperties ();
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
	
	// Spindle Config
	public static void setMyApp(javax.servlet.ServletContext myApp) {
		CONTEXT = myApp;
		CONTEXT_PATH = myApp.getRealPath("/");
	}

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
	
}