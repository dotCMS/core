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

package com.liferay.portlet.myaccount.action;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.liferay.portlet.ActionRequestImpl;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredUserException;
import com.liferay.portal.ReservedUserEmailAddressException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.UserSmsException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="UpdateProfileAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class UpdateProfileAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		String cmd = req.getParameter(Constants.CMD);

		if ((cmd != null) && (cmd.equals("main"))) {
			try {
				_updateMain(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof DuplicateUserEmailAddressException ||
					e instanceof ReservedUserEmailAddressException ||
					e instanceof UserEmailAddressException ||
					e instanceof UserFirstNameException ||
					e instanceof UserLastNameException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.edit_profile");
				}
				else if (e != null &&
						 e instanceof NoSuchUserException ||
						 e instanceof PrincipalException ||
						 e instanceof RequiredUserException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals("password"))) {
			try {
				_updatePassword(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof UserPasswordException) {

					UserPasswordException upe = (UserPasswordException)e;

					SessionErrors.add(req, e.getClass().getName(), upe);

					setForward(req, "portlet.my_account.edit_profile");
				}
				else if (e != null &&
						 e instanceof NoSuchUserException ||
						 e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals("comments"))) {
			try {
				_updateComments(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof NoSuchUserException ||
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals("sms"))) {
			try {
				_updateSms(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof UserSmsException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.edit_profile");
				}
				else if (e != null &&
						 e instanceof NoSuchUserException ||
						 e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if ((cmd != null) && (cmd.equals("im"))) {
			try {
				_updateIm(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof NoSuchUserException ||
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		
		else if ((cmd != null) && (cmd.equals("locale"))) {
			try {
				_updateLocale(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof NoSuchUserException ||
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		
		else if ((cmd != null) && (cmd.equals("favorites"))) {
			try {
				_updateFavorites(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof NoSuchUserException ||
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.my_account.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else {
			setForward(req, "portlet.my_account.edit_profile");
		}
	}

	private void _updateComments(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String comments = ParamUtil.getString(req, "comments");

		User user = PortalUtil.getSelectedUser(req);

		PortalUtil.updateUser(
			req, res, user.getUserId(), user.getFirstName(),
			user.getMiddleName(), user.getLastName(), user.getNickName(),
			user.isMale(), user.getBirthday(), user.getEmailAddress(),
			user.getSmsId(), user.getAimId(), user.getIcqId(), user.getMsnId(),
			user.getYmId(), user.getFavoriteActivity(),
			user.getFavoriteBibleVerse(), user.getFavoriteFood(),
			user.getFavoriteMovie(), user.getFavoriteMusic(),
			user.getLanguageId(), user.getTimeZoneId(), user.getSkinId(),
			user.getDottedSkins(), user.getRoundedSkins(), user.getGreeting(),
			user.getResolution(), user.getRefreshRate(), comments);

		// Session messages

		SessionMessages.add(req, "comments_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

	private void _updateFavorites(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String activity = ParamUtil.getString(req, "favorite_activity");
		String bibleVerse = ParamUtil.getString(req, "favorite_bible_verse");
		String food = ParamUtil.getString(req, "favorite_food");
		String movie = ParamUtil.getString(req, "favorite_movie");
		String music = ParamUtil.getString(req, "favorite_music");

		// Update model

		User user = PortalUtil.getSelectedUser(req);

		PortalUtil.updateUser(
			req, res, user.getUserId(), user.getFirstName(),
			user.getMiddleName(), user.getLastName(), user.getNickName(),
			user.isMale(), user.getBirthday(), user.getEmailAddress(),
			user.getSmsId(), user.getAimId(), user.getIcqId(), user.getMsnId(),
			user.getYmId(), activity, bibleVerse, food, movie, music,
			user.getLanguageId(), user.getTimeZoneId(), user.getSkinId(),
			user.getDottedSkins(), user.getRoundedSkins(), user.getGreeting(),
			user.getResolution(), user.getRefreshRate(), user.getComments());

		// Session messages

		SessionMessages.add(req, "favorites_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

	private void _updateIm(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String aimId = ParamUtil.getString(req, "aim_id");
		String icqId = ParamUtil.getString(req, "icq_id");
		String msnId = ParamUtil.getString(req, "msn_id");
		String ymId = ParamUtil.getString(req, "ym_id");

		User user = PortalUtil.getSelectedUser(req);

		PortalUtil.updateUser(
			req, res, user.getUserId(), user.getFirstName(),
			user.getMiddleName(), user.getLastName(), user.getNickName(),
			user.isMale(), user.getBirthday(), user.getEmailAddress(),
			user.getSmsId(), aimId, icqId, msnId, ymId,
			user.getFavoriteActivity(), user.getFavoriteBibleVerse(),
			user.getFavoriteFood(), user.getFavoriteMovie(),
			user.getFavoriteMusic(), user.getLanguageId(), user.getTimeZoneId(),
			user.getSkinId(), user.getDottedSkins(), user.getRoundedSkins(),
			user.getGreeting(), user.getResolution(), user.getRefreshRate(),
			user.getComments());

		// Session messages

		SessionMessages.add(req, "im_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}
	private void _updateLocale(ActionRequest req, ActionResponse res)
	throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

	String language = ParamUtil.getString(req, "user_language_id");
	String timezone = ParamUtil.getString(req, "user_tz_id");

	User user = PortalUtil.getSelectedUser(req);

	PortalUtil.updateUser(
		req, res, user.getUserId(), user.getFirstName(),
		user.getMiddleName(), user.getLastName(), user.getNickName(),
		user.isMale(), user.getBirthday(), user.getEmailAddress(),
		user.getSmsId(),  user.getAimId(), user.getIcqId(),
		user.getMsnId(), user.getYmId(),
		user.getFavoriteActivity(), user.getFavoriteBibleVerse(),
		user.getFavoriteFood(), user.getFavoriteMovie(),
		user.getFavoriteMusic(), language, timezone,
		user.getSkinId(), user.getDottedSkins(), user.getRoundedSkins(),
		user.getGreeting(), user.getResolution(), user.getRefreshRate(),
		user.getComments());

	// Session messages

	SessionMessages.add(req, "locale_updated");

	// Send redirect

	res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
}

	private void _updateMain(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String firstName = ParamUtil.getString(req, "first_name");
		String middleName = ParamUtil.getString(req, "middle_name");
		String lastName = ParamUtil.getString(req, "last_name");
		String nickName = ParamUtil.getString(req, "nick_name");

		String emailAddress = ParamUtil.getString(req, "email_address");

		boolean male = ParamUtil.get(req, "male", true);

		int bdMonth = ParamUtil.get(req, "birthday_month", Calendar.JANUARY);
		int bdDay = ParamUtil.get(req, "birthday_day", 1);
		int bdYear = ParamUtil.get(req, "birthday_year", 1970);

		User user = PortalUtil.getSelectedUser(req);

		String greeting = user.getGreeting();
		if (!user.getUserId().equals(PortalUtil.getUser(req).getUserId())) {
			greeting =
				LanguageUtil.get(user, "welcome") + ", " +
				User.getFullName(firstName, middleName, lastName) + "!";
		}

		PortalUtil.updateUser(
			req, res, user.getUserId(), firstName, middleName, lastName,
			nickName,  male,
			new GregorianCalendar(bdYear, bdMonth, bdDay).getTime(),
			emailAddress, user.getSmsId(), user.getAimId(), user.getIcqId(),
			user.getMsnId(), user.getYmId(), user.getFavoriteActivity(),
			user.getFavoriteBibleVerse(), user.getFavoriteFood(),
			user.getFavoriteMovie(),user.getFavoriteMusic(),
			user.getLanguageId(), user.getTimeZoneId(), user.getSkinId(),
			user.getDottedSkins(), user.getRoundedSkins(), greeting,
			user.getResolution(), user.getRefreshRate(), user.getComments());

		try {
			boolean active = ParamUtil.get(req, "active", true);

			UserManagerUtil.updateActive(user.getUserId(), active);
		}
		catch (PrincipalException pe) {
		}

		// Session messages

		SessionMessages.add(req, "main_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

	private void _updatePassword(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		PortletSession ses = req.getPortletSession();

		String password1 = ParamUtil.getString(req, "password_1");
		String password2 = ParamUtil.getString(req, "password_2");
		boolean passwordReset = ParamUtil.get(req, "password_reset", false);

		User user = PortalUtil.getSelectedUser(req);

		PortalUtil.updateUser(
			req, res, user.getUserId(), password1, password2, passwordReset);

		if (user.getUserId().equals(PortalUtil.getUser(req).getUserId())) {
			ses.setAttribute(
				WebKeys.USER_PASSWORD, password1,
				PortletSession.APPLICATION_SCOPE);
		}

		// Session messages

		SessionMessages.add(req, "password_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

	private void _updateSms(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String smsId = ParamUtil.getString(req, "sms_id");

		User user = PortalUtil.getSelectedUser(req);

		PortalUtil.updateUser(
			req, res, user.getUserId(), user.getFirstName(),
			user.getMiddleName(), user.getLastName(), user.getNickName(),
			user.isMale(), user.getBirthday(), user.getEmailAddress(), smsId,
			user.getAimId(), user.getIcqId(), user.getMsnId(), user.getYmId(),
			user.getFavoriteActivity(), user.getFavoriteBibleVerse(),
			user.getFavoriteFood(), user.getFavoriteMovie(),
			user.getFavoriteMusic(), user.getLanguageId(), user.getTimeZoneId(),
			user.getSkinId(), user.getDottedSkins(), user.getRoundedSkins(),
			user.getGreeting(), user.getResolution(), user.getRefreshRate(),
			user.getComments());

		// Session messages

		SessionMessages.add(req, "sms_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

}