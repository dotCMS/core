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

package com.liferay.portlet.admin.action;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.liferay.portal.CaptchaException;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.DuplicateUserIdException;
import com.liferay.portal.ReservedUserEmailAddressException;
import com.liferay.portal.ReservedUserIdException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserIdException;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.GetterUtil;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;
import com.octo.captcha.Captcha;

/**
 * <a href="CreateUserAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class CreateUserAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		try {
			PortletSession ses = req.getPortletSession();

			String companyId = PortalUtil.getCompanyId(req);

			String firstName = ParamUtil.getString(req, "first_name");
			String middleName = ParamUtil.getString(req, "middle_name");
			String lastName = ParamUtil.getString(req, "last_name");
			String nickName = ParamUtil.getString(req, "nick_name");

			String userId = ParamUtil.get(req, "user_id", "");
			boolean autoUserId = ParamUtil.get(req, "auto_user_id", false);

			String emailAddress =
				ParamUtil.getString(req, "email_address").toLowerCase();

			boolean autoPassword = ParamUtil.get(req, "auto_password", false);
			String password1 = ParamUtil.getString(req, "password_1");
			String password2 = ParamUtil.getString(req, "password_2");
			boolean passwordReset = ParamUtil.get(
				req, "password_reset",
				GetterUtil.getBoolean(PropsUtil.get(
					PropsUtil.PASSWORDS_CHANGE_ON_FIRST_USE)));

			boolean male = ParamUtil.get(req, "male", true);

			int bdMonth = ParamUtil.get(
				req, "birthday_month", Calendar.JANUARY);
			int bdDay = ParamUtil.get(req, "birthday_day", 1);
			int bdYear = ParamUtil.get(req, "birthday_year", 1970);

			Locale locale = null;
			if (req.getRemoteUser() == null) {
				locale = (Locale)ses.getAttribute(
					Globals.LOCALE_KEY, PortletSession.APPLICATION_SCOPE);
			}
			else {
				User defaultUser =
					UserLocalManagerUtil.getDefaultUser(companyId);

				locale = defaultUser.getLocale();
			}

			Captcha captcha = null;

			if (GetterUtil.getBoolean(
					PropsUtil.get(PropsUtil.CAPTCHA_CHALLENGE)) &&
				(config.getPortletName().equals(PortletKeys.MY_ACCOUNT))) {

				captcha = (Captcha)ses.getAttribute(
					WebKeys.CAPTCHA, PortletSession.APPLICATION_SCOPE);

				Boolean validResponse = captcha.validateResponse(
					ParamUtil.getString(req, "captcha_response"));

				if ((validResponse == null) ||
					(validResponse.equals(Boolean.FALSE))) {

					ses.removeAttribute(
						WebKeys.CAPTCHA, PortletSession.APPLICATION_SCOPE);

					throw new CaptchaException();
				}
			}

			UserManagerUtil.addUser(
				companyId, autoUserId, userId, autoPassword, password1,
				password2, passwordReset, firstName, middleName, lastName,
				nickName, male,
				new GregorianCalendar(bdYear, bdMonth, bdDay).getTime(),
				emailAddress, locale);

			if (captcha != null) {
				captcha.disposeChallenge();

				ses.removeAttribute(
					WebKeys.CAPTCHA, PortletSession.APPLICATION_SCOPE);
			}

			// Session messages

			SessionMessages.add(req, CreateUserAction.class.getName());

			// Send redirect

			res.sendRedirect(ParamUtil.getString(req, "redirect"));
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof CaptchaException ||
				e instanceof DuplicateUserEmailAddressException ||
				e instanceof DuplicateUserIdException ||
				e instanceof ReservedUserEmailAddressException ||
				e instanceof ReservedUserIdException ||
				e instanceof UserEmailAddressException ||
				e instanceof UserFirstNameException ||
				e instanceof UserIdException ||
				e instanceof UserLastNameException) {

				SessionErrors.add(req, e.getClass().getName());

				setForward(req, "portlet.admin.list_users");
			}
			else if (e != null &&
					 e instanceof UserPasswordException) {

				UserPasswordException upe = (UserPasswordException)e;

				SessionErrors.add(req, e.getClass().getName(), upe);

				setForward(req, "portlet.admin.list_users");
			}
			else if (e != null &&
					 e instanceof PrincipalException) {

				SessionErrors.add(req, e.getClass().getName());

				setForward(req, "portlet.admin.error");
			}
			else {
				req.setAttribute(PageContext.EXCEPTION, e);

				setForward(req, Constants.COMMON_ERROR);
			}
		}
	}

}