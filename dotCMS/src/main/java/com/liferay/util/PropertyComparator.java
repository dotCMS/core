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

import com.dotmarketing.util.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * <a href="PropertyComparator.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Patrick Brady
 * @version $Revision: 1.2 $
 *
 */
public class PropertyComparator implements Comparator {

	public PropertyComparator(String propertyName) {
		_propertyNames = new String[]{propertyName};
	}

	public PropertyComparator(String[] propertyNames) {
		_propertyNames = propertyNames;
	}

	public int compare(Object obj1, Object obj2) {
		try {
			for (int i = 0; i < _propertyNames.length; i++) {
				String propertyName = _propertyNames[i];

				Object property1 =
					PropertyUtils.getProperty(obj1, propertyName);
				Object property2 =
					PropertyUtils.getProperty(obj2, propertyName);

				if (property1 instanceof String) {
					int result = property1.toString().compareToIgnoreCase(
						property2.toString());

					if (result != 0) {
						return result;
					}
				}

				if (property1 instanceof Comparable) {
					int result = ((Comparable)property1).compareTo(property2);

					if (result != 0) {
						return result;
					}
				}
			}
		}
		catch (NoSuchMethodException nsme) {
			Logger.error(this,nsme.getMessage(),nsme);
		}
		catch (InvocationTargetException ite) {
			Logger.error(this,ite.getMessage(),ite);
		}
		catch (IllegalAccessException iae) {
			Logger.error(this,iae.getMessage(),iae);
		}

		return -1;
	}

	private String[] _propertyNames;

}