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

import java.util.Comparator;

/**
 * <a href="KeyValuePairComparator.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class KeyValuePairComparator implements Comparator {

	public KeyValuePairComparator(boolean byKey, boolean asc) {
		_byKey = true;
		_asc = asc;
	}

	public int compare(Object obj1, Object obj2) {
		KeyValuePair kvp1 = (KeyValuePair)obj1;
		KeyValuePair kvp2 = (KeyValuePair)obj2;

		if (_byKey) {
			String key1 = kvp1.getKey();
			String key2 = kvp2.getKey();

			if (_asc) {
				return key1.compareTo(key2);
			}
			else {
				return -(key1.compareTo(key2));
			}
		}
		else {
			String value1 = kvp1.getValue();
			String value2 = kvp2.getValue();

			if (_asc) {
				return value1.compareTo(value2);
			}
			else {
				return -(value1.compareTo(value2));
			}
		}
	}

	private boolean _byKey;
	private boolean _asc;

}