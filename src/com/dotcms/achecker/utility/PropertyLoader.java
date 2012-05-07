package com.dotcms.achecker.utility;


import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import com.dotcms.achecker.impl.ACheckerImpl;

public class PropertyLoader {
	
	private static Properties prop = null;
	
	private static final String RESOURCE_NAME = "achecker.properties";

	public static String[] getArray(String key) {
		String value = getValue(key);
		if (value == null) {
			return new String[0];
		}
		else {
			return StringUtils.split(value);
		}
	}

	public static String getValue(String key) {
		return  initLoader().getProperty(key);
	}


	private static Properties initLoader() {
		// if (prop == null) {
			prop = new Properties();
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			try {
				URL url = classLoader.getResource( RESOURCE_NAME );
				url = ACheckerImpl.class.getResource("/achecker.properties");
				if (url != null) {
					InputStream is = url.openStream();
					prop.load(is);
					is.close();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		// }
		return prop;
		 
	}
 
}
