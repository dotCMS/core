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

import java.lang.reflect.Method;
import java.util.Map;

import com.liferay.util.CollectionFactory;

/**
 * <a href="MethodCache.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Michael C. Han
 * @version $Revision: 1.6 $
 *
 */
public class MethodCache {

	public static Method get(MethodKey methodKey)
		throws ClassNotFoundException, NoSuchMethodException {

		return _getInstance()._get(methodKey);
	}

	private static MethodCache _getInstance() {
		if (_instance == null) {
			synchronized (MethodCache.class) {
				if (_instance == null) {
					_instance = new MethodCache();
				}
			}
		}

		return _instance;
	}

	private MethodCache() {
		_classes = CollectionFactory.getSyncHashMap();
		_methods = CollectionFactory.getSyncHashMap();
	}

	private Method _get(MethodKey methodKey)
		throws ClassNotFoundException, NoSuchMethodException {

		Method method = (Method)_methods.get(methodKey);

		if (method == null) {
			String className = methodKey.getClassName();
			String methodName = methodKey.getMethodName();
			Class[] types = methodKey.getTypes();

			Class classObj = (Class)_classes.get(className);

			if (classObj == null) {
				classObj = Class.forName(className);
				_classes.put(className, classObj);
			}

			method = classObj.getMethod(methodName, types);
			_methods.put(methodKey, method);
		}

		return method;
	}

	private static MethodCache _instance;

	private Map _classes;
	private Map _methods;

}