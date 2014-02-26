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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.liferay.portlet.ActionRequestImpl;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredUserException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="DeleteUserAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class DeleteUserAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		try {

            // Getting the http request
            ActionRequestImpl reqImpl = (ActionRequestImpl) req;
            HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

			User user = PortalUtil.getSelectedUser(req);

			UserManagerUtil.deleteUser(user.getUserId());

			// Session messages

			SessionMessages.add(req, DeleteUserAction.class.getName());

			// Send redirect

			res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof NoSuchUserException ||
				e instanceof PrincipalException ||
				e instanceof RequiredUserException) {

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