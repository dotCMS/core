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

package com.liferay.util.lang;

import java.io.Serializable;

import com.liferay.util.StringPool;

/**
 * <a href="MethodKey.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class MethodKey implements Serializable {

	public MethodKey(String className, String methodName, Class[] types) {
		_className = className;
		_methodName = methodName;
		_types = types;
	}

	public String getClassName() {
		return _className;
	}

	public String getMethodName() {
		return _methodName;
	}

	public Class[] getTypes() {
		return _types;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		MethodKey methodKey = (MethodKey)obj;

		if (toString().equals(methodKey.toString())) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String toString() {
		return _toString();
	}

	private String _toString() {
		if (_toString == null) {
			StringBuffer sb = new StringBuffer();
			sb.append(_className);
			sb.append(_methodName);

			if (_types != null && _types.length > 0) {
				sb.append(StringPool.DASH);

				for (int i = 0; i < _types.length; i++) {
					sb.append(_types[i].getClass().getName());
				}
			}

			_toString = sb.toString();
		}

		return _toString;
	}

	private String _className;
	private String _methodName;
	private Class[] _types;
	private String _toString;

}