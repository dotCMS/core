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

package com.liferay.portlet.pollsdisplay.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.polls.NoSuchDisplayException;
import com.liferay.portlet.polls.ejb.PollsDisplayManagerUtil;
import com.liferay.portlet.polls.model.PollsDisplay;
import com.liferay.util.InstancePool;

/**
 * <a href="ViewAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class ViewAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		try {
//			Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);

			PollsDisplay display = PollsDisplayManagerUtil.getDisplay(config.getPortletName());

			req.setAttribute(WebKeys.POLLS_DISPLAY, display);

			return mapping.findForward("portlet.polls_display.view");
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof NoSuchDisplayException) {

				req.removeAttribute(WebKeys.POLLS_DISPLAY);

				PortletAction pa = (PortletAction)InstancePool.get(
					SetupAction.class.getName());

				return pa.render(mapping, form, config, req, res);
			}
			else {
				req.setAttribute(PageContext.EXCEPTION, e);

				return mapping.findForward(Constants.COMMON_ERROR);
			}
		}
	}

}