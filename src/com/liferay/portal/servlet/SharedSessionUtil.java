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

package com.liferay.portal.servlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;

import com.liferay.portal.util.WebKeys;
import com.liferay.util.CollectionFactory;

/**
 * <a href="SharedSessionUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Michael Weisser
 * @version $Revision: 1.5 $
 *
 */
public class SharedSessionUtil {

	public static String[] SHARED_SESSION_ATTRIBUTES = new String[] {
		Globals.LOCALE_KEY, WebKeys.COMPANY_ID, WebKeys.USER_ID,
		WebKeys.USER_PASSWORD
	};

	public static Map getSharedSessionAttributes(HttpServletRequest req) {
		Map sharedSessionAttributes = CollectionFactory.getSyncHashMap();

		HttpSession ses = req.getSession();

		for (int i = 0; i < SHARED_SESSION_ATTRIBUTES.length; i++) {
			String attrName = SHARED_SESSION_ATTRIBUTES[i];
			Object attrValue = ses.getAttribute(attrName);

			if (attrValue != null) {
				sharedSessionAttributes.put(attrName, attrValue);
			}
		}

		return sharedSessionAttributes;
	}

}