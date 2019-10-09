/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotcms.repackage.javax.portlet.PortletPreferences;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.Action;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.ConcretePortletWrapper;
import com.liferay.portlet.LiferayWindowState;
import com.liferay.portlet.RenderParametersPool;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.UploadServletRequest;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

/**
 * <a href="LayoutAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.17 $
 *
 */
public class LayoutAction extends Action {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {

    if (req.getParameter("p_p_id") != null) {
      try {
        String action = req.getParameter("p_p_action");

        if (action != null && action.equals("1")) {
          String companyId = PortalUtil.getCompanyId(req);
          User user = PortalUtil.getUser(req);
          String portletId = ParamUtil.getString(req, "p_p_id");

          Portlet portlet = PortletManagerUtil.getPortletById(companyId, portletId);

          // boolean hasPermission = RoleLocalManagerUtil.hasRoles(user.getUserId(), portlet.getRolesArray());
          // if (!hasPermission) {
          // _processRenderRequest(req, res);
          // }else{
          _processActionRequest(req, res);

          if (!Boolean.parseBoolean(req.getParameter("isNg"))) {
            ActionResponseImpl actionResponse = (ActionResponseImpl) req.getAttribute(WebKeys.JAVAX_PORTLET_RESPONSE);

            String redirectLocation = actionResponse.getRedirectLocation();

            if (Validator.isNotNull(redirectLocation)) {
              res.sendRedirect(SecurityUtils.stripReferer(req, redirectLocation));

              return null;
            }
          }

          String windowState = req.getParameter("p_p_state");

          if (LiferayWindowState.EXCLUSIVE.toString().equals(windowState)) {

            return null;
          }
          // }
        } else if (action != null && action.equals("0")) {
          _processRenderRequest(req, res);
        }

        Layout layout = (Layout) req.getAttribute(WebKeys.LAYOUT);

        if (layout != null) {
          _updateLayout(req, layout);
        }

        return mapping.findForward("portal.layout");
      } catch (Exception e) {
        req.setAttribute(PageContext.EXCEPTION, e);

        return mapping.findForward(Constants.COMMON_ERROR);
      }
    } else {
      try {
        _forwardLayout(req);

        return mapping.findForward(Constants.COMMON_FORWARD);
      } catch (Exception e) {
        req.setAttribute(PageContext.EXCEPTION, e);

        return mapping.findForward(Constants.COMMON_ERROR);
      }
    }
  }

  private void _forwardLayout(HttpServletRequest req) throws Exception {
    HttpSession ses = req.getSession();

    Layout layout = (Layout) req.getAttribute(WebKeys.LAYOUT);

    String layoutId = null;

    if (layout == null || !InodeUtils.isSet(layoutId)) {
      User user = PortalUtil.getUser(req);
      List<Layout> userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
      if (userLayouts != null && userLayouts.size() > 0) {
        layoutId = userLayouts.get(0).getId();
      }
    } else {
      layoutId = layout.getId();
    }

    String ctxPath = (String) ses.getAttribute(WebKeys.CTX_PATH);

    req.setAttribute(WebKeys.FORWARD_URL, ctxPath + "/portal" + PortalUtil.getAuthorizedPath(req) + "/layout?p_l_id=" + layoutId);
  }

  private void _processPortletRequest(HttpServletRequest req, HttpServletResponse res, boolean action) throws Exception {

    String contentType = req.getHeader("Content-Type");

    if ((contentType != null) && (contentType.startsWith("multipart/form-data"))) {

      UploadServletRequest uploadReq = (UploadServletRequest) req;

      req = uploadReq;
    }

    String companyId = PortalUtil.getCompanyId(req);
    User user = PortalUtil.getUser(req);
    Layout layout = (Layout) req.getAttribute(WebKeys.LAYOUT);
    String portletId = ParamUtil.getString(req, "p_p_id");

    Portlet portlet = PortletManagerUtil.getPortletById(companyId, portletId);

    ServletContext ctx = (ServletContext) req.getAttribute(WebKeys.CTX);

    ConcretePortletWrapper concretePortletWrapper = (ConcretePortletWrapper) APILocator.getPortletAPI().getImplementingInstance(portlet);


    PortletPreferences portletPrefs = null;

    PortletConfig portletConfig = APILocator.getPortletAPI().getPortletConfig(portlet);
    PortletContext portletCtx = portletConfig.getPortletContext();

    WindowState windowState = new WindowState(ParamUtil.getString(req, "p_p_state"));

    PortletMode portletMode = new PortletMode(ParamUtil.getString(req, "p_p_mode"));

    if (action) {
      ActionRequestImpl actionRequest =
          new ActionRequestImpl(req, portlet, concretePortletWrapper, portletCtx, windowState, portletMode, portletPrefs, layout.getId());

      ActionResponseImpl actionResponse = new ActionResponseImpl(actionRequest, res, portletId, user, layout, windowState, portletMode);

      actionRequest.defineObjects(portletConfig, actionResponse);

      concretePortletWrapper.processAction(actionRequest, actionResponse);

      RenderParametersPool.put(req, layout.getId(), portletId, actionResponse.getRenderParameters());
    } else {
      // PortalUtil.updateWindowState(portletId, user, layout, windowState);
      //
      // PortalUtil.updatePortletMode(portletId, user, layout, portletMode);
    }
  }

  private void _processActionRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

    _processPortletRequest(req, res, true);
  }

  private void _processRenderRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

    _processPortletRequest(req, res, false);
  }

  private void _updateLayout(HttpServletRequest req, Layout layout) throws Exception {

  }

}
