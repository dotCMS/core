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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <a href="MethodInvoker.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class MethodInvoker {

	public static Object invoke(MethodWrapper methodWrapper)
		throws ClassNotFoundException, IllegalAccessException,
			   InstantiationException, InvocationTargetException,
			   NoSuchFieldException, NoSuchMethodException {

		String className = methodWrapper.getClassName();
		String methodName = methodWrapper.getMethodName();
		Object args[] = methodWrapper.getArgs();

		List parameterTypes = new ArrayList();

		for (int i = 0; i < args.length; i++) {
			Class argClass = args[i].getClass();

			if (ClassUtil.isSubclass(argClass, PrimitiveWrapper.class)) {
				parameterTypes.add(argClass.getField("TYPE").get(args[i]));

				MethodKey methodKey = new MethodKey(
					argClass.getName(), "getValue", null);

				Method method = MethodCache.get(methodKey);

				args[i] = method.invoke(args[i], null);
			}
			else if (args[i] instanceof NullWrapper) {
				NullWrapper nullWrapper = (NullWrapper)args[i];

				parameterTypes.add(Class.forName(nullWrapper.getClassName()));

				args[i] = null;
			}
			else {
				parameterTypes.add(argClass);
			}
		}

		Object objClass = Class.forName(className).newInstance();

		Method method = null;

		try {
			MethodKey methodKey = new MethodKey(
				methodWrapper.getClassName(), methodWrapper.getMethodName(),
				(Class[])parameterTypes.toArray(new Class[0]));

			method = MethodCache.get(methodKey);
		}
		catch (NoSuchMethodException nsme) {
			Method[] methods = objClass.getClass().getMethods();

			for (int i = 0; i < methods.length; i++) {
				Class[] methodParameterTypes = methods[i].getParameterTypes();

				if (methods[i].getName().equals(methodName) &&
					methodParameterTypes.length == parameterTypes.size()) {

					boolean correctParams = true;

					for (int j = 0; j < parameterTypes.size(); j++) {
						Class a = (Class)parameterTypes.get(j);
						Class b = methodParameterTypes[j];

						if (!ClassUtil.isSubclass(a, b)) {
							correctParams = false;

							break;
						}
					}

					if (correctParams) {
						method = methods[i];

						break;
					}
				}
			}

			if (method == null) {
				throw nsme;
			}
		}

		Object returnObj = null;

		if (method != null) {
			returnObj = method.invoke(objClass, args);
		}

		return returnObj;
	}

}