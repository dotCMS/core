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

package com.liferay.portal;

import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="LayoutFriendlyURLException.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.183 $
 *
 */
public class LayoutFriendlyURLException extends PortalException {

	public static final int DOES_NOT_START_WITH_SLASH = 1;

	public static final int ENDS_WITH_SLASH = 2;

	public static final int TOO_SHORT = 3;

	public static final int ADJACENT_SLASHES = 4;

	public static final int INVALID_CHARACTERS = 5;

	public static final int DUPLICATE = 6;

	public static int validate(String friendlyURL) {
		if (friendlyURL.length() < 2) {
			return TOO_SHORT;
		}

		if (!friendlyURL.startsWith(StringPool.SLASH)) {
			return DOES_NOT_START_WITH_SLASH;
		}

		if (friendlyURL.endsWith(StringPool.SLASH)) {
			return ENDS_WITH_SLASH;
		}

		if (friendlyURL.indexOf("//") != -1) {
			return ADJACENT_SLASHES;
		}

		char[] c = friendlyURL.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if ((!Validator.isChar(c[i])) && (!Validator.isDigit(c[i])) &&
				(c[i] != '/') && (c[i] != '-') && (c[i] != '_')) {

				return INVALID_CHARACTERS;
			}
		}

		return -1;
	}

	public LayoutFriendlyURLException(int type) {
		_type = type;
	}

	public int getType() {
		return _type;
	}

	private int _type;

}