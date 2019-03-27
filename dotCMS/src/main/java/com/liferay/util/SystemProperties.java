/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import com.dotmarketing.util.Logger;

/**
 * <a href="SystemProperties.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Mirco Tamburini
 * @author  Brett Randall
 * @version $Revision: 1.5 $
 *
 */
public class SystemProperties {

	public static final String SYSTEM_PROPERTIES_LOAD =
		"system.properties.load";

	public static final String SYSTEM_PROPERTIES_FINAL =
		"system.properties.final";

	public static String get(String key) {
		String value = (String)_getInstance()._props.get(key);

		if (value == null) {
			value = System.getProperty(key);
		}

		return value;
	}

	public static void set(String key, String value) {
		_getInstance()._props.put(key, value);
	}

	public static String[] getArray(String key) {
		String value = get(key);

		if (value == null) {
			return new String[0];
		}
		else {
			return StringUtil.split(value);
		}
	}

	public static Properties getProperties() {
		return PropertiesUtil.fromMap(_getInstance()._props);
	}

	private static SystemProperties _getInstance() {
		if (_instance == null) {
			synchronized (SystemProperties.class) {
				if (_instance == null) {
					_instance = new SystemProperties();
				}
			}
		}

		return _instance;
	}

	private SystemProperties() {
		Properties p = new Properties();

		ClassLoader classLoader = getClass().getClassLoader();

		// system.properties

		try {
			URL url = classLoader.getResource("system.properties");

			if (url != null) {
				InputStream is = url.openStream();

				p.load(is);

				is.close();

				Logger.info(this, "Loading " + url);
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		// system-ext.properties

		try {
			URL url = classLoader.getResource("system-ext.properties");

			if (url != null) {
				InputStream is = url.openStream();

				p.load(is);

				is.close();

				Logger.info(this, "Loading " + url);
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		// Set system properties

		boolean systemPropertiesLoad = GetterUtil.get(
			System.getProperty(SYSTEM_PROPERTIES_LOAD), true);

		boolean systemPropertiesFinal = GetterUtil.get(
			System.getProperty(SYSTEM_PROPERTIES_FINAL), true);

		if (systemPropertiesLoad) {
			Enumeration enu = p.propertyNames();

			while (enu.hasMoreElements()) {
				String key = (String)enu.nextElement();

				if (systemPropertiesFinal ||
					Validator.isNull(System.getProperty(key))) {

					System.setProperty(key, (String)p.get(key));
				}
			}
		}

		// Use a fast synchronized hash map implementation instead of the slower
		// java.util.Properties

		PropertiesUtil.fromProperties(p, _props);
	}

	private static SystemProperties _instance;

	private Map _props = CollectionFactory.getSyncHashMap();

}