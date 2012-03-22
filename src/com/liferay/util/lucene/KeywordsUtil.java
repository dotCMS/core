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

package com.liferay.util.lucene;

import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;

/**
 * <a href="KeywordsUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Mirco Tamburini
 * @author  Josiah Goh
 * @version $Revision: 1.4 $
 *
 */
public class KeywordsUtil {

	public static final String[] SPECIAL = new String[] {
		"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~",
		"*", "?", ":", "\\"
	};

	public static String escape(String text) {
		for (int i = SPECIAL.length - 1; i >= 0; i--) {
			text = StringUtil.replace(
				text, SPECIAL[i], StringPool.BACK_SLASH + SPECIAL[i]);
		}

		return text;
	}

	public static String toWildcard(String keywords) {
		if (keywords == null) {
			return null;
		}

		/*if (!keywords.startsWith(StringPool.STAR)) {
			keywords = StringPool.STAR + keywords;
		}*/

		if (!keywords.endsWith(StringPool.STAR)) {
			keywords = keywords + StringPool.STAR;
		}

		return keywords;
	}

}