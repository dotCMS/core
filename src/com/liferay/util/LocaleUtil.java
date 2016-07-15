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

import java.util.Locale;

import com.dotcms.repackage.org.apache.struts.Globals;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <a href="LocaleUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class LocaleUtil {

	public static Locale fromLanguageId(String languageId) {
		Locale locale = null;

		try {
			int pos = languageId.indexOf(StringPool.UNDERLINE);

			String languageCode = languageId.substring(0, pos);
			String countryCode = languageId.substring(
				pos + 1, languageId.length());

			locale = new Locale(languageCode, countryCode);
		}
		catch (Exception e) {
			_log.warn(languageId + " is not a valid language id");
		}

		if (locale == null) {
			locale = Locale.getDefault();
		}

		return locale;
	}

	// todo: unit test me
	/**
	 * Get Locale based on the arguments country and language, if both are null will try to get it from the {@link Globals} LOCALE_KEY,
	 * if the LOCALE_KEY is also null, will get the request default one.
	 *
	 * If country or language are not null (one of them could be null), will build a new locale and set to the session under {@link Globals} LOCALE_KEY
	 * @param request
	 * @param country
	 * @param language
     * @return
     */
	public static Locale getLocale (final HttpServletRequest request, final String country, final String language) {

		final HttpSession session = request.getSession();
		Locale locale = null;

		try {

			if (null == country && null == language) {

				if (null != session.getAttribute(Globals.LOCALE_KEY)) {

					return (Locale) session.getAttribute(Globals.LOCALE_KEY);
				} else {

					locale = request.getLocale();
				}
			} else {

				final Locale.Builder builder =
						new Locale.Builder();

				if (null != language) {

					builder.setLanguage(language);
				}

				if (null != country) {

					builder.setRegion(country);
				}

				locale = builder.build();
			}
		} catch (Exception e) {

			locale = Locale.getDefault();
		}

		if (null != locale) {

			session.setAttribute(Globals.LOCALE_KEY, locale);
		}

		return locale;
	}
	private static final Log _log = LogFactory.getLog(LocaleUtil.class);

}