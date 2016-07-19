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

import com.dotcms.repackage.org.apache.hadoop.security.User;
import com.dotcms.util.security.*;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;
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

	private static com.dotcms.util.security.Encryptor encryptor = null;

	private static UserAPI userAPI = null;

	private static UserWebAPI userWebAPI = null;

	public static void setEncryptor(Encryptor encryptor) {
		LocaleUtil.encryptor = encryptor;
	}

	public static void setUserAPI(UserAPI userAPI) {
		LocaleUtil.userAPI = userAPI;
	}

	public static void setUserWebAPI(UserWebAPI userWebAPI) {
		LocaleUtil.userWebAPI = userWebAPI;
	}

	private static Encryptor getEncryptor() {

		if (null == encryptor) {
			synchronized (LocaleUtil.class) {

				if (null == encryptor) {
					encryptor =
							EncryptorFactory.getInstance().getEncryptor();
				}
			}
		}

		return encryptor;
	}

	private static UserAPI getUserAPI() {

		if (null == userAPI) {
			synchronized (LocaleUtil.class) {

				if (null == userAPI) {
					userAPI =
							APILocator.getUserAPI();
				}
			}
		}

		return userAPI;
	}

	private static UserWebAPI getUserWebAPI() {

		if (null == userWebAPI) {
			synchronized (LocaleUtil.class) {

				if (null == userWebAPI) {
					userWebAPI =
							WebAPILocator.getUserWebAPI();
				}
			}
		}
		return userWebAPI;
	}

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

		final HttpSession session = request.getSession(false);
		Locale locale = null;

		try {

			processLocaleUserCookie (request);

			if (!UtilMethods.isSet(country) && !UtilMethods.isSet(language)) {

				if (null != session && null != session.getAttribute(Globals.LOCALE_KEY)) {

					return (Locale) session.getAttribute(Globals.LOCALE_KEY);
				} else {

					locale = request.getLocale();
				}
			} else {

				final Locale.Builder builder =
						new Locale.Builder();

				if (UtilMethods.isSet(language)) {

					builder.setLanguage(language);
				}

				if (UtilMethods.isSet(country)) {

					builder.setRegion(country);
				}

				locale = builder.build();
			}
		} catch (Exception e) {

			if (null != session && null != session.getAttribute(Globals.LOCALE_KEY)) {

				locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
			} else if (null != request.getLocale()) {

				locale = request.getLocale();
			} else {
				locale = Locale.getDefault();
			}
		}

		if (null != locale) {

			session.setAttribute(Globals.LOCALE_KEY, locale);
		}

		return locale;
	}

	/**
	 * This method is called just to set the locale from the user saved on a
	 * @param request
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotDataException
	 * @throws DotSecurityException
     */
	private static void processLocaleUserCookie(final HttpServletRequest request) throws SystemException, PortalException, DotDataException, DotSecurityException {

		String uId = null;

		final Cookie[] cookies =
				request.getCookies();
		final HttpSession session =
				request.getSession(false);

		if(cookies != null) {

			uId = CookieUtil.get
					(request.getCookies(), CookieKeys.ID);

			if(UtilMethods.isSet(uId)) {

				try {

					uId = getEncryptor().decryptString(uId);
				} catch(Exception e) {

					_log.info("An invalid attempt to login as " + uId + " has been made from IP: "
									+ request.getRemoteAddr());
					uId = null;
				}
			}
		}

		if(UtilMethods.isSet(uId)) {

			if (null != session) {
				session.setAttribute(WebKeys.USER_ID, uId);

				//DOTCMS-4943
				final boolean respectFrontend =
						getUserWebAPI().isLoggedToBackend(request);
				final com.liferay.portal.model.User loggedInUser =
						getUserAPI().loadUserById(uId, getUserAPI().getSystemUser(), respectFrontend);
				session.setAttribute(com.dotcms.repackage.org.apache.struts.Globals.LOCALE_KEY,
						loggedInUser.getLocale());
			}
		}

	}

	private static final Log _log = LogFactory.getLog(LocaleUtil.class);

}