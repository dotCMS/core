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

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.util.InputStreamUtils;
import com.dotmarketing.util.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * <a href="PropertiesUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class PropertiesUtil {

	/**
	 * Extension for .properties Prop file
	 */
	public static  final String PROP_EXT = ".properties";

	/**
	 * Extension for .xml Prop file
	 */
	public static final String  XML_EXT  = ".xml";

	public static void copyProperties(Properties from, Properties to) {
		Iterator itr = from.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			to.setProperty((String)entry.getKey(), (String)entry.getValue());
		}
	}

	public static Properties fromMap(Map map) {
		Properties p = new Properties();

		Iterator itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			p.setProperty((String)entry.getKey(), (String)entry.getValue());
		}

		return p;
	}

	public static void fromProperties(Properties p, Map map) {
		map.clear();

		Iterator itr = p.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			map.put(entry.getKey(), entry.getValue());
		}
	}

	public static Map fromProperties(Properties p) {

		final Map map = new HashMap();
		final Iterator itr = p.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			map.put(entry.getKey(), entry.getValue());
		}

		return map;
	}

	public static void load(Properties p, String s) throws IOException {
		s = UnicodeFormatter.toString(s);
		s = StringUtil.replace(s, "\\u003d", "=");
		s = StringUtil.replace(s, "\\u000a", "\n");

		p.load(new ByteArrayInputStream(s.getBytes()));
	}

	/**
	 * Load the properties, it could be a xml or properties.
	 * @param resourceName String.
	 * @return Properties
	 */
	public static Properties load (final String resourceName) {

		Properties properties = null;

		if (null != resourceName) {

			properties = (resourceName.endsWith(XML_EXT))?
					loadXML(resourceName):
					loadProperties(resourceName);
		}

		return properties;
	} // load.

	/**
	 * Load the properties,  .properties file.
	 * @param resourceName String.
	 * @return Properties
     */
	public static Properties loadProperties (final String resourceName) {

		InputStream inputStream = null;
		Properties properties = null;

		try {
			// use the class name as a path to find the properties
			inputStream =
					InputStreamUtils.getInputStream(resourceName);

			if (null != inputStream) {

				properties =
						new Properties();

				properties.load(new BufferedInputStream(inputStream));
			} else {

				Logger.error(PropertiesUtil.class,
						"No Properties File loaded for the resourceName: " + resourceName);
			}
		} catch (Exception e) {

			Logger.error(PropertiesUtil.class, e.getMessage(), e);
		} finally {

			IOUtils.closeQuietly(inputStream);
		}

		return properties;
	} // loadProperties.

	/**
	 * Load the properties (.XML file).
	 * @param resourceName String.
	 * @return Properties
	 */
	public static Properties loadXML (final String resourceName) {

		InputStream inputStream = null;
		Properties properties = null;

		try {

			// use the class name as a path to find the properties
			inputStream =
					InputStreamUtils.getInputStream(resourceName);

			if (null != inputStream) {

				properties =
						new Properties();

				properties.loadFromXML(new BufferedInputStream(inputStream));

			} else {

				Logger.error(PropertiesUtil.class, "No Properties File loaded for the resourceName: " + resourceName);
			}
		} catch (Exception e) {

			Logger.error(PropertiesUtil.class, e.getMessage(), e);
		} finally {

			IOUtils.closeQuietly(inputStream);
		}

		return properties;
	} // loadXML.
}