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

package com.liferay.portlet;

import java.util.Map;
import java.util.Random;

/**
 * <a href="CustomUserAttributes.java.html"><b><i>View Source</i></b></a>
 *
 * <p>
 * A separate instance of this class is created every time
 * <code>renderRequest.getAttribute(PortletRequest.USER_INFO)</code> is called.
 * It is safe to cache attributes in this instance because you can assume that
 * all calls to this instance belong to the same user.
 * </p>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class CustomUserAttributes implements Cloneable {

	public String getValue(String name, Map userInfo) {
		if (name == null) {
			return null;
		}

		if (name.equals("user.name.random")) {
			String[] names = new String[] {"Aaa", "Bbb", "Ccc"};

			return names[new Random().nextInt(3)];
		}
		else {
			return null;
		}
	}

	public Object clone() {
		return new CustomUserAttributes();
	}

}