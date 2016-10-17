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

package com.liferay.portal.language;

import java.util.Locale;

import javax.servlet.jsp.PageContext;

import com.liferay.portal.model.User;
import com.liferay.util.UnicodeFormatter;

/**
 * <a href="UnicodeLanguageUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class UnicodeLanguageUtil {

	public static String get(User user, String key) throws LanguageException {
		return UnicodeFormatter.toString(LanguageUtil.get(user, key));
	}

	public static String get(String companyId, Locale locale, String key)
		throws LanguageException {

		return UnicodeFormatter.toString(
			LanguageUtil.get(companyId, locale, key));
	}

	public static String get(PageContext pageContext, String key)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.get(pageContext, key));
	}

	public static String format(
			PageContext pageContext, String pattern, Object argument)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, argument));
	}

	public static String format(
			PageContext pageContext, String pattern, Object argument,
			boolean translateArguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, argument, translateArguments));
	}

	public static String format(
			PageContext pageContext, String pattern, Object[] arguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, arguments));
	}

	public static String format(
			PageContext pageContext, String pattern, Object[] arguments,
			boolean translateArguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, arguments, translateArguments));
	}

	public static String format(
			PageContext pageContext, String pattern, LanguageWrapper argument)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, argument));
	}

	public static String format(
			PageContext pageContext, String pattern, LanguageWrapper argument,
			boolean translateArguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, argument, translateArguments));
	}

	public static String format(
			PageContext pageContext, String pattern,
			LanguageWrapper[] arguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, arguments));
	}

	public static String format(
			PageContext pageContext, String pattern,
			LanguageWrapper[] arguments, boolean translateArguments)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.format(
			pageContext, pattern, arguments, translateArguments));
	}

	public static String getTimeDescription(
			PageContext pageContext, Long milliseconds)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.getTimeDescription(
			pageContext, milliseconds));
	}

	public static String getTimeDescription(
			PageContext pageContext, long milliseconds)
		throws LanguageException {

		return UnicodeFormatter.toString(LanguageUtil.getTimeDescription(
			pageContext, milliseconds));
	}

}