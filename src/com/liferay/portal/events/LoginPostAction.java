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

package com.liferay.portal.events;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;

import com.liferay.portal.model.User;
import com.liferay.portal.model.UserTracker;
import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.BrowserSniffer;
import com.liferay.util.GetterUtil;

/**
 * <a href="LoginPostAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.24 $
 *
 */
public class LoginPostAction extends Action {

	public void run(HttpServletRequest req, HttpServletResponse res)
		throws ActionException {

		try {
			HttpSession ses = req.getSession();

			String companyId = PortalUtil.getCompanyId(req);
			User user = PortalUtil.getUser(req);

			Map currentUsers =
				(Map)WebAppPool.get(companyId, WebKeys.CURRENT_USERS);

			boolean simultaenousLogins = GetterUtil.get(
				PropsUtil.get(PropsUtil.AUTH_SIMULTANEOUS_LOGINS), true);

			if (!simultaenousLogins) {
				Map.Entry[] currentUsersArray =
					(Map.Entry[])currentUsers.entrySet().toArray(
						new Map.Entry[0]);

				for (int i = 0; i < currentUsersArray.length; i++) {
					Map.Entry mapEntry = currentUsersArray[i];

					UserTracker userTracker = (UserTracker)mapEntry.getValue();

					if (userTracker.getUserId().equals(user.getUserId())) {

						// Disable old login

						userTracker.getHttpSession().setAttribute(
							WebKeys.STALE_SESSION, new Boolean(true));
					}
				}
			}

			UserTracker userTracker =
				(UserTracker)currentUsers.get(ses.getId());

			if (userTracker == null) {
				userTracker = new UserTracker(
					ses.getId(), companyId, PortalUtil.getUser(req).getUserId(), new Date(),
					req.getRemoteAddr(), req.getRemoteHost(),
					req.getHeader("USER-AGENT"));

				userTracker.setHttpSession(ses);

				currentUsers.put(ses.getId(), userTracker);
			}

//			if (!GetterUtil.getBoolean(PropsUtil.get(
//					PropsUtil.LAYOUT_REMEMBER_WINDOW_STATE_MAXIMIZED))) {

//				Iterator itr = user.getLayouts().iterator();
//
//				while (itr.hasNext()) {
//					Layout layout = (Layout)itr.next();
//
//					if (layout.hasStateMax()) {
//
//						// Set the window state to normal for the maximized
//						// portlet
//
//						layout.setStateMax(StringPool.BLANK);
//
//						// Set the portlet mode to view because other modes may
//						// require a maximized window state
//
//						layout.setModeEdit(StringPool.BLANK);
//						layout.setModeHelp(StringPool.BLANK);
//
//						LayoutManagerUtil.updateLayout(
//							layout.getPrimaryKey(), layout.getName(),
//							layout.getColumnOrder(), layout.getNarrow1(),
//							layout.getNarrow2(), layout.getWide(),
//							layout.getStateMax(), layout.getStateMin(),
//							layout.getModeEdit(), layout.getModeHelp());
//					}
//				}
//			}

			if (BrowserSniffer.is_ns_4(req)) {

				// Netscape 4.x users should never see dotted or rounded skins

				boolean dottedSkins = false;
				boolean roundedSkins = false;

				PortalUtil.updateUser(
					req, res, user.getUserId(), user.getFirstName(),
					user.getMiddleName(), user.getLastName(),
					user.getNickName(), user.isMale(), user.getBirthday(),
					user.getEmailAddress(), user.getSmsId(), user.getAimId(),
					user.getIcqId(), user.getMsnId(), user.getYmId(),
					user.getFavoriteActivity(), user.getFavoriteBibleVerse(),
					user.getFavoriteFood(), user.getFavoriteMovie(),
					user.getFavoriteMusic(), user.getLanguageId(),
					user.getTimeZoneId(), user.getSkinId(), dottedSkins,
					roundedSkins, user.getGreeting(), user.getResolution(),
					user.getRefreshRate(), user.getComments());
			}

			// Reset the locale

			ses.removeAttribute(Globals.LOCALE_KEY);
		}
		catch (Exception e) {
			throw new ActionException(e);
		}
	}

}