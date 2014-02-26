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
import javax.portlet.PreferencesValidator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.liferay.portlet.ActionRequestImpl;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.NoSuchPortletException;
import com.liferay.portal.PortletActiveException;
import com.liferay.portal.PortletDefaultPreferencesException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.PortletPreferencesSerializer;
import com.liferay.util.InstancePool;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.lucene.Indexer;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="EditPortletAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class EditPortletAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		String cmd = req.getParameter(Constants.CMD);

		try {
			_editPortlet(req);
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof NoSuchPortletException) {

				SessionErrors.add(req, e.getClass().getName());

				setForward(req, "portlet.admin.error");
			}
			else {
				req.setAttribute(PageContext.EXCEPTION, e);

				setForward(req, Constants.COMMON_ERROR);
			}

			return;
		}

		if (cmd != null && cmd.equals(Constants.UPDATE)) {
			try {
				_updatePortlet(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof PortletActiveException ||
					e instanceof PortletDefaultPreferencesException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.admin.edit_portlet");
				}
				else if (e != null &&
					e instanceof NoSuchPortletException ||
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
		else if ((cmd != null) && (cmd.equals(Constants.SEARCH))) {
			try {
				_updatePortletIndex(req);

				setForward(req, "portlet.admin.edit_portlet");
			}
			catch (Exception e) {
				req.setAttribute(PageContext.EXCEPTION, e);

				setForward(req, Constants.COMMON_ERROR);
			}
		}
		else {
			setForward(req, "portlet.admin.edit_portlet");
		}
	}

	private void _editPortlet(ActionRequest req) throws Exception {
		String portletId = ParamUtil.getString(req, "portlet_id");

		String groupId = ParamUtil.getString(req, "group_id");

		Portlet portlet = PortletManagerUtil.getPortletById(
			PortalUtil.getCompanyId(req), groupId, portletId);

		if (portlet == null) {
			throw new NoSuchPortletException();
		}

		req.setAttribute(WebKeys.PORTLET, portlet);
	}

	private void _updatePortlet(ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String portletId = ParamUtil.getString(req, "portlet_id");

		String groupId = ParamUtil.getString(req, "group_id");
		String defaultPreferences = ParamUtil.getString(
			req, "portlet_default_prefs");
		boolean narrow = ParamUtil.getBoolean(req, "portlet_narrow");
		String roles = ParamUtil.getString(req, "portlet_roles");
		boolean active = ParamUtil.getBoolean(req, "portlet_active");

		Portlet portlet = PortletManagerUtil.getPortletById(
			PortalUtil.getCompanyId(req), portletId);

		PreferencesValidator prefsValidator =
			PortalUtil.getPreferencesValidator(portlet);

		if (prefsValidator != null) {
			try {
				prefsValidator.validate(
					PortletPreferencesSerializer.fromDefaultXML(
						defaultPreferences));
			}
			catch (Exception e) {
				throw new PortletDefaultPreferencesException();
			}
		}

		PortletManagerUtil.updatePortlet(
			portletId, groupId, defaultPreferences, narrow, roles, active);

		// Session messages

		SessionMessages.add(req, "portlet_updated");

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
	}

	private void _updatePortletIndex(ActionRequest req) throws Exception {
		String portletId = ParamUtil.getString(req, "portlet_id");

		String companyId = PortalUtil.getCompanyId(req);

		Portlet portlet =
			PortletManagerUtil.getPortletById(companyId, portletId);

		if (Validator.isNotNull(portlet.getIndexerClass())) {
			Indexer indexer =
				(Indexer)InstancePool.get(portlet.getIndexerClass());

			indexer.reIndex(new String[] {companyId});
		}

		// Session messages

		SessionMessages.add(req, "portlet_index_updated");
	}

}