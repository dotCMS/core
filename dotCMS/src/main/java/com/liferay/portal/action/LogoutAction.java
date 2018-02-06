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

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * <a href="LogoutAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 * 
 */
public class LogoutAction extends Action {

	private final LoginServiceAPI loginService = APILocator.getLoginServiceAPI();

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {

		try {
			try {
				// Logger.info(this, "User " +
				// PortalUtil.getUser(req).getFullName() + " (" +
				// PortalUtil.getUser(req).getUserId() +
				// ") has logged out from IP: " + req.getRemoteAddr());
				SecurityLogger.logInfo(this.getClass(), "User " + PortalUtil.getUser(req).getFullName() +
						" (" + PortalUtil.getUser(req).getUserId() + ") has logged out from IP: "
						+ req.getRemoteAddr());
			} catch (Exception e) {
				//Logger.info(this, "User has logged out from IP: " + req.getRemoteAddr());
				SecurityLogger.logInfo(this.getClass(),"User has logged out from IP: " + req.getRemoteAddr());
			}

			try {
				this.loginService.doActionLogout(req, res);
			} catch (Exception e) {
			}

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