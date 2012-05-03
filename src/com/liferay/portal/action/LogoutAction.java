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

package com.liferay.portal.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.servlet.PortletSessionPool;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.CookieUtil;
import com.liferay.util.StringPool;

/**
 * <a href="LogoutAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 * 
 */
public class LogoutAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {

		try {
			HttpSession ses = req.getSession();
			try {
				// Logger.info(this, "User " +
				// PortalUtil.getUser(req).getFullName() + " (" +
				// PortalUtil.getUser(req).getUserId() +
				// ") has logged out from IP: " + req.getRemoteAddr());
				SecurityLogger.logInfo(this.getClass(), "User " + PortalUtil.getUser(req).getFullName() + " (" + PortalUtil.getUser(req).getUserId() + ") has logged out from IP: "
						+ req.getRemoteAddr());
			} catch (Exception e) {
				//Logger.info(this, "User has logged out from IP: " + req.getRemoteAddr());
				SecurityLogger.logInfo(this.getClass(),"User has logged out from IP: " + req.getRemoteAddr());
			}

			EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_PRE), req, res);

			ArrayList<Cookie> al = new ArrayList<Cookie>();
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (int i = 0; i < cookies.length; i++) {
					Cookie cookie = cookies[i];
					al.add(cookie);
					cookie.setMaxAge(0);
					cookie.setPath("/");
					res.addCookie(cookie);
				}
			}

			Map sessions = PortletSessionPool.remove(ses.getId());

			if (sessions != null) {
				Iterator itr = sessions.entrySet().iterator();

				while (itr.hasNext()) {
					Map.Entry entry = (Map.Entry) itr.next();

					HttpSession portletSession = (HttpSession) entry.getValue();

					portletSession.invalidate();
				}
			}

			try {
				ses.invalidate();
			} catch (Exception e) {
			}

			EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_POST), req, res);

			// ActionForward af = mapping.findForward("referer");
			// return af;
			return mapping.findForward(Constants.COMMON_REFERER);
		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_REFERER);
		}
	}

	private static final Log _log = LogFactory.getLog(LogoutAction.class);

}