/**
 * 
 */
package com.liferay.portlet.admin.action;

import com.dotcms.repackage.portlet.javax.portlet.ActionRequest;
import com.dotcms.repackage.portlet.javax.portlet.ActionResponse;
import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.cache.NavMenuCache;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionErrors;

/**
 * @author Carlos Rivas
 *
 */
public class CustomEditPortletAction extends EditPortletAction {
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		String cmd = req.getParameter(Constants.CMD);
	
		if( (cmd != null && cmd.equals(Constants.UPDATE)) ) {
			String portletId = ParamUtil.getString(req, "portlet_id");
			String groupId = ParamUtil.getString(req, "group_id");
			Portlet portlet = PortletManagerUtil.getPortletById(
				PortalUtil.getCompanyId(req), groupId, portletId);
			String currentDefaultPreferences = portlet.getDefaultPreferences();

			super.processAction(mapping, form, config, req, res);
			
			if( SessionErrors.size(req) == 0 ) {
				String newDefaultPreferences = ParamUtil.getString(
						req, "portlet_default_prefs");
				if( !newDefaultPreferences.equals(currentDefaultPreferences) ) {
					NavMenuCache.invalidate(portletId);
				}
			}
		}
		else {
			super.processAction(mapping, form, config, req, res);
		}
	}
}
