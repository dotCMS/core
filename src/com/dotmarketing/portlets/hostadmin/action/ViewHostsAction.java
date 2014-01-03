package com.dotmarketing.portlets.hostadmin.action;

import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;
import com.dotcms.repackage.portlet.javax.portlet.RenderRequest;
import com.dotcms.repackage.portlet.javax.portlet.RenderResponse;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author David Torres
 * @version 1.9
 * 
 */
public class ViewHostsAction extends DotPortletAction {

	HostAPI hostAPI;
	ContentletAPI contentAPI = APILocator.getContentletAPI();
	
	public ViewHostsAction () throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		hostAPI = APILocator.getHostAPI();
		//Interceptor conI = (Interceptor)APILocator.getContentletAPIntercepter();
		//conI.addPostHook("com.dotmarketing.portlets.hostadmin.business.CopyHostContentPostHook");
	}

	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

		Logger.debug(this, "Running ViewHostsAction!!!!");

		return mapping.findForward("portlet.ext.hostadmin.view_hosts");
		
	}

}
