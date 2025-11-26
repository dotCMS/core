/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.utility;


import com.dotcms.enterprise.achecker.impl.ACheckerImpl;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;

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
