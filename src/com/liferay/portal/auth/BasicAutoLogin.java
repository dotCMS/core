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

package com.liferay.portal.auth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.CookieUtil;
import com.liferay.util.KeyValuePair;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="BasicAutoLogin.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class BasicAutoLogin implements AutoLogin {

	public String[] login(HttpServletRequest req, HttpServletResponse res)
		throws AutoLoginException {

		
		if(req.getQueryString()!=null){
			if(req.getQueryString().contains("_struts_action") && req.getQueryString().contains("p_l_id")){
				Cookie testCookie = new Cookie("backend_login_return_url",req.getQueryString());
				
				testCookie.setPath("/");
				testCookie.setMaxAge(100);
				res.addCookie(testCookie);
			}
		}
		
		try {
			
			String[] credentials = null;

			String autoUserId = CookieUtil.get(req.getCookies(), CookieKeys.ID);
			String autoPassword =
				CookieUtil.get(req.getCookies(), CookieKeys.PASSWORD);

			if (Validator.isNotNull(autoUserId) &&
				Validator.isNotNull(autoPassword)) {

				Company company = PortalUtil.getCompany(req);

				KeyValuePair kvp = null;
				
				if (company.isAutoLogin()) {
					kvp = UserManagerUtil.decryptUserId(
						company.getCompanyId(), autoUserId, autoPassword);

					credentials = new String[3];

					credentials[0] = kvp.getKey();
					credentials[1] = kvp.getValue();
					credentials[2] = Boolean.FALSE.toString();
				}
			}

			return credentials;
		}
		catch (Exception e) {
			Cookie cookie = new Cookie(CookieKeys.ID, StringPool.BLANK);
			cookie.setMaxAge(0);
			cookie.setPath("/");

			res.addCookie(cookie);

			cookie = new Cookie(CookieKeys.PASSWORD, StringPool.BLANK);
			cookie.setMaxAge(0);
			cookie.setPath("/");

			res.addCookie(cookie);

			throw new AutoLoginException(e);
		}
	}

}