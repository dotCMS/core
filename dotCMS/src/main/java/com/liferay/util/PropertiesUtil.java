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

import com.dotcms.util.ConfigurationInterpolator;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

	private static ConfigurationInterpolator interpolator =
			SystemEnvironmentConfigurationInterpolator.INSTANCE;

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

			if (null != entry.getValue()) {

				map.put(entry.getKey(), interpolator.interpolate(entry.getValue().toString()));
			} else {

				map.put(entry.getKey(), entry.getValue());
			}
		}
	}



	public static void load(Properties p, String s) throws IOException {
		s = UnicodeFormatter.toString(s);
		s = StringUtil.replace(s, "\\u003d", "=");
		s = StringUtil.replace(s, "\\u000a", "\n");

		p.load(new ByteArrayInputStream(s.getBytes()));
	}

}