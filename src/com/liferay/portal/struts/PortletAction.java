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

package com.liferay.portal.struts;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import com.liferay.portal.util.WebKeys;

/**
 * <a href="PortletAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class PortletAction extends Action {

	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest req,
			HttpServletResponse res)
		throws Exception {

		PortletConfig portletConfig =
			(PortletConfig)req.getAttribute(WebKeys.JAVAX_PORTLET_CONFIG);

		RenderRequest renderRequest =
			(RenderRequest)req.getAttribute(WebKeys.JAVAX_PORTLET_REQUEST);

		RenderResponse renderResponse =
			(RenderResponse)req.getAttribute(WebKeys.JAVAX_PORTLET_RESPONSE);

		return render(
			mapping, form, portletConfig, renderRequest, renderResponse);
	}

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {
	}

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		return mapping.findForward(getForward(req));
	}

	public String getForward(RenderRequest req) {
		return getForward(req, null);
	}

	public String getForward(RenderRequest req, String defaultValue) {
		String forward =
			(String)req.getAttribute(WebKeys.PORTLET_STRUTS_FORWARD);

		if (forward == null) {
			return defaultValue;
		}
		else {
			return forward;
		}
	}

	public void setForward(ActionRequest req, String forward) {
		req.setAttribute(WebKeys.PORTLET_STRUTS_FORWARD, forward);
	}

	protected MessageResources getResources() {
		ServletContext ctx = getServlet().getServletContext();

		return (MessageResources)ctx.getAttribute(Globals.MESSAGES_KEY);
	}

	protected MessageResources getResources(HttpServletRequest req) {
		return getResources();
	}

	protected MessageResources getResources(ActionRequest req) {
		return getResources();
	}

	protected MessageResources getResources(RenderRequest req) {
		return getResources();
	}

}