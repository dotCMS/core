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

package com.liferay.util.servlet;

import java.security.Key;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="EncryptedServletRequest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class EncryptedServletRequest extends HttpServletRequestWrapper {

	public EncryptedServletRequest(HttpServletRequest req, Key key) {
		super(req);

		_params = new HashMap();
		_key = key;

		Enumeration enu = getParameterNames();

		while (enu.hasMoreElements()) {
			String name = (String)enu.nextElement();
			String[] values = super.getParameterValues(name);

			for (int i = 0; i < values.length; i++) {
				if (Validator.isNotNull(values[i])) {
					try {
						values[i] = Encryptor.decrypt(_key, values[i]);
					}
					catch (EncryptorException ee) {
						values[i] = StringPool.BLANK;
					}
				}
			}

			_params.put(name, values);
		}
	}

	public String getParameter(String name) {
		String[] values = (String[])_params.get(name);

		if ((values != null) && (values.length > 0)) {
			return values[0];
		}
		else {
			return null;
		}
	}

	public Map getParameterMap() {
		return Collections.unmodifiableMap(_params);
	}

	public String[] getParameterValues(String name) {
		return (String[])_params.get(name);
	}

	private Map _params;
	private Key _key;

}