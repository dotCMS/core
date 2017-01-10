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

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletSession;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.InstancePool;

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

		// Authenticated users can never create a new account

		if (req.getRemoteUser() != null) {

            // Getting the http request
            ActionRequestImpl reqImpl = (ActionRequestImpl) req;
            HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

			// Send redirect

			String ctxPath =
				(String)req.getPortletSession().getAttribute(
					WebKeys.CTX_PATH, PortletSession.APPLICATION_SCOPE);

			res.sendRedirect(SecurityUtils.stripReferer(httpReq, ctxPath));
		}

		String cmd = req.getParameter(Constants.CMD);

		if (cmd != null) {
			PortletAction pa = (PortletAction)InstancePool.get(
				com.liferay.portlet.admin.action.CreateUserAction.class.
					getName());

			pa.processAction(mapping, form, config, req, res);
		}
		else {
			setForward(req, "portlet.admin.list_users");
		}
	}

}