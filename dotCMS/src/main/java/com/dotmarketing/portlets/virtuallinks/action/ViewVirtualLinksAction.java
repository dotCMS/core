package com.dotmarketing.portlets.virtuallinks.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;

/**
 *
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class ViewVirtualLinksAction extends PortletAction {
	
	private VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

	/**
	 * 
	 * @param mapping
	 *            - Contains the mapping of a particular request to an instance
	 *            of a particular action class.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param res
	 *            - The HTTP Response wrapper.
	 * @throws Exception
	 * 
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {
        Logger.debug(this, "Running ViewVirtualLinksAction!!!!");
        User user=(User)req.getAttribute("USER");
        HostAPI hostAPI = APILocator.getHostAPI();
        List<Host> hosts= hostAPI.getHostsWithPermission(PermissionAPI.PERMISSION_READ, false, user, false);
        
        VirtualLinkAPI.OrderBy orderby = VirtualLinkAPI.OrderBy.TITLE;
        String temp = req.getParameter("orderby");
        if ((temp!= null) && temp.equals("url")) {
        	orderby = VirtualLinkAPI.OrderBy.URL;
        }
        com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        String hostId = (String) httpReq.getSession().getAttribute(WebKeys.CMS_SELECTED_HOST_ID);
        String url = null;
        if (UtilMethods.isSet(hostId)) {
        	Host host = hostAPI.find(hostId, user, false);
        	if (host != null){
        		url = host.getHostname() + ":/";
        	}
        	else{
        		url = "there is no host named this" + ":/";
        	}
        }

		try {
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				//get their lists
				List<VirtualLink> list = virtualLinkAPI.getVirtualLinks(null, url, orderby);
				req.setAttribute(WebKeys.VIRTUAL_LINK_VIEW_PORTLET, list);
				req.setAttribute("host_list", hosts);
		        Logger.debug(this, "Going to: portlet.ext.virtuallinks.view");
				return mapping.findForward("portlet.ext.virtuallinks.view");
			}
			else {
				if (UtilMethods.isSet(req.getParameter("query"))){
					if(UtilMethods.isSet(req.getParameter("host_name"))){
						hostId =req.getParameter("host_name");
					}
					Host h = hostAPI.find(hostId, user, false);
					List<VirtualLink> listVar = virtualLinkAPI.getVirtualLinks(req.getParameter("query"), url, orderby);
					List<VirtualLink> listV = new ArrayList<VirtualLink>(); 
					Iterator listvs = listVar.iterator();
					if(h != null && InodeUtils.isSet(h.getInode()))
					{
						while (listvs .hasNext()) {
							VirtualLink next = (VirtualLink) listvs .next();
							if (next.getUrl().toLowerCase().startsWith(h.getHostname().toLowerCase()) || (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user))){
								listV.add(next);
							} 
						}
								
					}		
					else{
						while (listvs .hasNext()) {
							VirtualLink next = (VirtualLink) listvs .next();
							if (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user)){
								listV.add(next);
							} 
						}
					}
					
					int totalVLinksToDisplay = listV.size();
					req.setAttribute(WebKeys.VIRTUAL_LINK_LIST_VIEW, listV);
					req.setAttribute("host_list", hosts);
					return mapping.findForward("portlet.ext.virtuallinks.view_virtuallinks");
				}
				else{
					List<VirtualLink> listTemp = new ArrayList<VirtualLink>();
					List<VirtualLink> list = new ArrayList<VirtualLink>();
					Host h = hostAPI.find(hostId, user, false);
					if (InodeUtils.isSet(req.getParameter("inode"))) {
						IHTMLPage htmlPage = (IHTMLPage) InodeFactory.getInode(req.getParameter("inode"),IHTMLPage.class);
						Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
						listTemp = this.virtualLinkAPI.getIncomingVirtualLinks(identifier.getURI());
					}
					else{
						if (UtilMethods.isSet(url)) {
							listTemp = virtualLinkAPI.getVirtualLinks(null, url, orderby);
						} else {
							listTemp = virtualLinkAPI.getVirtualLinks(null, hosts, orderby);
						}
					}
					Iterator listvs = listTemp.iterator();
					if(h != null && InodeUtils.isSet(h.getInode()))
					{
						while (listvs .hasNext()) {
							VirtualLink next = (VirtualLink) listvs .next();
							if (next.getUrl().toLowerCase().startsWith(h.getHostname().toLowerCase()) || (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user))){
								list.add(next);
							} 
						}
					}else{
						while (listvs .hasNext()) {
							VirtualLink next = (VirtualLink) listvs .next();
							if (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user)){
								list.add(next);
							} 
						}
					}
					req.setAttribute(WebKeys.VIRTUAL_LINK_LIST_VIEW, list);
					req.setAttribute("host_list", hosts);
					return mapping.findForward("portlet.ext.virtuallinks.view_virtuallinks");
				}
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
	
}
