/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
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

package com.dotmarketing.portlets.virtuallinks.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class ViewVirtualLinksAction extends PortletAction {
	
	private VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
	
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
        if ((temp!= null) && temp.equals("url"))
        	orderby = VirtualLinkAPI.OrderBy.URL;
        
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
				List list = virtualLinkAPI.getVirtualLinks(null, url, orderby);
				//list =VirtualLinkFactory.checkListForCreateVirtualLinkspermission(list, user);
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
							if (next.getUrl().startsWith(h.getHostname()) || (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user))){
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
					
					//listV =VirtualLinkFactory.checkListForCreateVirtualLinkspermission(listV, user);
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
						HTMLPage htmlPage = (HTMLPage) InodeFactory.getInode(req.getParameter("inode"),HTMLPage.class);
						Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
						listTemp = VirtualLinkFactory.getIncomingVirtualLinks(identifier.getURI());
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
							if (next.getUrl().startsWith(h.getHostname()) || (next.getUrl().startsWith("/") && APILocator.getUserAPI().isCMSAdmin(user))){
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
					
					
					
//					if(APILocator.getUserAPI().isCMSAdmin(user)) {
//						List<VirtualLink> listAux = new ArrayList<VirtualLink>();
//						listAux = virtualLinkAPI.getVirtualLinks(null, "/", orderby);
//						for(VirtualLink vlink : listAux){
//							if (vlink.getUrl().startsWith("/")){
//								if(!list.contains(vlink)){
//								  list.add(vlink);
//								}
//							}
//						}
//					}
					//list =VirtualLinkFactory.checkListForCreateVirtualLinkspermission(list, user);
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