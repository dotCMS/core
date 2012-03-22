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

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.WindowState;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.CachePortlet;
import com.liferay.portlet.LiferayWindowState;
import com.liferay.portlet.RenderParametersPool;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.UploadServletRequest;

/**
 * <a href="LayoutAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.17 $
 *
 */
public class LayoutAction extends Action {

	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest req,
			HttpServletResponse res)
		throws Exception {

		if (req.getParameter("p_l_id") != null) {
			try {
				String action = req.getParameter("p_p_action");

				if (action != null && action.equals("1")) {
					String companyId = PortalUtil.getCompanyId(req);
					User user = PortalUtil.getUser(req);
					String portletId = ParamUtil.getString(req, "p_p_id");

					Portlet portlet =
						PortletManagerUtil.getPortletById(companyId, portletId);
					
//						boolean hasPermission = RoleLocalManagerUtil.hasRoles(user.getUserId(), portlet.getRolesArray());
//						if (!hasPermission) {
//							_processRenderRequest(req, res);
//						}else{
							_processActionRequest(req, res);

							ActionResponseImpl actionResponse =
								(ActionResponseImpl)req.getAttribute(
									WebKeys.JAVAX_PORTLET_RESPONSE);

							String redirectLocation =
								actionResponse.getRedirectLocation();

							if (Validator.isNotNull(redirectLocation)) {
								res.sendRedirect(redirectLocation);

								return null;
							}

							String windowState = req.getParameter("p_p_state");

							if (LiferayWindowState.EXCLUSIVE.toString().equals(
									windowState)) {

								return null;
							}
//						}
					}else if (action != null && action.equals("0")) {
					_processRenderRequest(req, res);
				}
				
				Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);

				if (layout != null) {
					_updateLayout(req, layout);
				}

				return mapping.findForward("portal.layout");
			}
			catch (Exception e) {
				req.setAttribute(PageContext.EXCEPTION, e);

				return mapping.findForward(Constants.COMMON_ERROR);
			}
		}
		else {
			try {
				_forwardLayout(req);

				return mapping.findForward(Constants.COMMON_FORWARD);
			}
			catch (Exception e) {
				req.setAttribute(PageContext.EXCEPTION, e);

				return mapping.findForward(Constants.COMMON_ERROR);
			}
		}
	}

	private void _forwardLayout(HttpServletRequest req) throws Exception {
		HttpSession ses = req.getSession();

		Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);
		
		
		String layoutId = null;

		if (layout == null || !InodeUtils.isSet(layoutId)) {
			User user = PortalUtil.getUser(req);
			List<Layout> userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
			if(userLayouts != null && userLayouts.size() > 0){
				layoutId = userLayouts.get(0).getId();
			}
		}else{
			layoutId = layout.getId();
		}

		String ctxPath = (String)ses.getAttribute(WebKeys.CTX_PATH);

		req.setAttribute(
			WebKeys.FORWARD_URL,
			ctxPath + "/portal" + PortalUtil.getAuthorizedPath(req) +
				"/layout?p_l_id=" + layoutId);
	}

	private void _processPortletRequest(
			HttpServletRequest req, HttpServletResponse res, boolean action)
		throws Exception {

		HttpSession ses = req.getSession();

		String contentType = req.getHeader("Content-Type");

		if ((contentType != null) &&
			(contentType.startsWith("multipart/form-data"))) {

			UploadServletRequest uploadReq = (UploadServletRequest)req;

			req = uploadReq;
		}

		String companyId = PortalUtil.getCompanyId(req);
		User user = PortalUtil.getUser(req);
		Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);
		String portletId = ParamUtil.getString(req, "p_p_id");

		Portlet portlet =
			PortletManagerUtil.getPortletById(companyId, portletId);

		ServletContext ctx = (ServletContext)req.getAttribute(WebKeys.CTX);

		CachePortlet cachePortlet = PortalUtil.getPortletInstance(portlet, ctx);

		if (user != null) {
			CachePortlet.clearResponse(ses, layout, portletId);
		}

//		PortletPreferences portletPrefs =
//			PortletPreferencesManagerUtil.getPreferences(
//				companyId, PortalUtil.getPortletPreferencesPK(req, portletId));

		PortletPreferences portletPrefs = null;
		
		PortletConfig portletConfig = PortalUtil.getPortletConfig(portlet, ctx);
		PortletContext portletCtx = portletConfig.getPortletContext();

		WindowState windowState = new WindowState(
			ParamUtil.getString(req, "p_p_state"));

		PortletMode portletMode = new PortletMode(
			ParamUtil.getString(req, "p_p_mode"));

		if (action) {
			ActionRequestImpl actionRequest = new ActionRequestImpl(
				req, portlet, cachePortlet, portletCtx, windowState,
				portletMode, portletPrefs, layout.getId());

			ActionResponseImpl actionResponse = new ActionResponseImpl(
				actionRequest, res, portletId, user, layout, windowState,
				portletMode);

			actionRequest.defineObjects(portletConfig, actionResponse);

			cachePortlet.processAction(actionRequest, actionResponse);

			RenderParametersPool.put(
				req, layout.getId(), portletId,
				actionResponse.getRenderParameters());
		}
		else {
//			PortalUtil.updateWindowState(portletId, user, layout, windowState);
//
//			PortalUtil.updatePortletMode(portletId, user, layout, portletMode);
		}
	}

	private void _processActionRequest(
			HttpServletRequest req, HttpServletResponse res)
		throws Exception {

		_processPortletRequest(req, res, true);
	}

	private void _processRenderRequest(
			HttpServletRequest req, HttpServletResponse res)
		throws Exception {

		_processPortletRequest(req, res, false);
	}

	private void _updateLayout(HttpServletRequest req, Layout layout)
		throws Exception {

//		HttpSession ses = req.getSession();
//
//		// Make sure portlets show up in the correct columns
//
//		boolean updateLayout = false;
//
//		if (layout.getNumOfColumns() > 1) {
//			Portlet[] narrow1Portlets = layout.getNarrow1Portlets();
//
//			for (int i = 0; i < narrow1Portlets.length; i++) {
//				if (!narrow1Portlets[i].isNarrow()) {
//					layout.removePortletId(narrow1Portlets[i].getPortletId());
//					layout.addPortletId(narrow1Portlets[i].getPortletId());
//
//					updateLayout = true;
//				}
//			}
//
//			Portlet[] narrow2Portlets = layout.getNarrow2Portlets();
//
//			for (int i = 0; i < narrow2Portlets.length; i++) {
//				if (!narrow2Portlets[i].isNarrow()) {
//					layout.removePortletId(narrow2Portlets[i].getPortletId());
//					layout.addPortletId(narrow2Portlets[i].getPortletId());
//
//					updateLayout = true;
//				}
//			}
//
//			Portlet[] widePortlets = layout.getWidePortlets();
//
//			for (int i = 0; i < widePortlets.length; i++) {
//				if (widePortlets[i].isNarrow()) {
//					layout.removePortletId(widePortlets[i].getPortletId());
//					layout.addPortletId(widePortlets[i].getPortletId());
//
//					updateLayout = true;
//				}
//			}
//		}
//
//		// See action path /my_account/edit_profile
//
//		if (layout.hasStateMax()) {
//			String maxPortletId = StringUtil.split(layout.getStateMax())[0];
//			String selPortletId = ParamUtil.getString(req, "p_p_id");
//
//			if ((!selPortletId.equals(maxPortletId)) &&
//				(!layout.hasPortletId(maxPortletId))) {
//
//				layout.setStateMax(StringPool.BLANK);
//				layout.setModeEdit(StringPool.BLANK);
//				layout.setModeHelp(StringPool.BLANK);
//
//				updateLayout = true;
//			}
//		}
//
//		if (updateLayout) {
//			CachePortlet.clearResponses(ses);
//
//			// Update layout without checking for permissions
//
//			LayoutLocalManagerUtil.updateLayout(
//				layout.getPrimaryKey(), layout.getName(),
//				layout.getColumnOrder(), layout.getNarrow1(),
//				layout.getNarrow2(), layout.getWide(),
//				layout.getStateMax(), layout.getStateMin(),
//				layout.getModeEdit(), layout.getModeHelp());
//		}
	}

}