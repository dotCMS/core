/**
 * 
 */
package com.liferay.portlet.admin.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

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
