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
import java.util.Map;
import java.util.Properties;

import com.dotmarketing.util.Logger;

/**
 * <a href="ExtPropertiesLoader.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ExtPropertiesLoader {

	public void init(String name) {
		Properties p = new Properties();

		ClassLoader classLoader = getClass().getClassLoader();

		try {
			URL url = classLoader.getResource(name + ".properties");

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

		try {
			URL url = classLoader.getResource(name + "-ext.properties");

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

		// Use a fast synchronized hash map implementation instead of the slower
		// java.util.Properties

		PropertiesUtil.fromProperties(p, _props);
	}

	public boolean containsKey(String key) {
		return _props.containsKey(key);
	}

	public String get(String key) {
		return (String)_props.get(key);
	}

	public void set(String key, String value) {
		_props.put(key, value);
	}

	public String[] getArray(String key) {
		String value = get(key);

		if (value == null) {
			return new String[0];
		}
		else {
			return StringUtil.split(value);
		}
	}

	public Properties getProperties() {
		return PropertiesUtil.fromMap(_props);
	}

	private Map _props = CollectionFactory.getSyncHashMap();

}