package com.dotmarketing.portlets.virtuallinks.action;

import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.portlets.virtuallinks.struts.VirtualLinkForm;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria
 */

public class EditVirtualLinkAction extends DotPortletAction {
	
	private static VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {
	
		String cmd = req.getParameter(Constants.CMD);
        Logger.debug(this, "Inside EditVirtualLinkAction cmd=" + cmd);
         
		//get the user
		User user = _getUser(req);
		HostAPI hostAPI = APILocator.getHostAPI();
		
	    List<Host> hosts = hostAPI.getHostsWithPermission(com.dotmarketing.business.PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS, false, user, false);
	    
	    req.setAttribute("host_list", hosts);
	    req.setAttribute("isCMSAdministrator", new Boolean(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole().getId())));
	    
		/*
		 *  get the mainglist object, stick it in request
		 *  
		 */
		try {
	        Logger.debug(this, "I'm retrieving the list");
			_retrieveVirtualLink(req, res, config, form);
		}
		catch (Exception ae) {
			_handleException(ae, req);
		}

		/*
		 *  if we are saving, 
		 *  
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				if (Validator.validate(req,form,mapping)) {
			        Logger.debug(this, "I'm Saving the virtual link");
					_saveVirtualLink(req, res, config, form, user);
					_sendToReferral(req, res, "");
				}
			}
			catch (Exception ae) {
				if (!ae.getMessage().equals(WebKeys.UNIQUE_VIRTUAL_LINK_EXCEPTION)){
					_handleException(ae, req);
				}
			}
		}

		/*
		 * deleting the list, return to listing page
		 *  
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
		        Logger.debug(this, "I'm deleting the virtual link");
				_deleteVirtualLink(req, res, config, form,user);

			}
			catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, "");
		}

		/*
		 * Copy copy props from the db to the form bean 
		 * 
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
		    VirtualLink vl = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);
			BeanUtils.copyProperties(form, req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT));
			String linkUrl = vl.getUrl();
			if (linkUrl != null) {
			    String[] splittedLink = linkUrl.split(":");
			    if (splittedLink.length > 1) {
			        Host host = hostAPI.findByName(splittedLink[0], user, false);
			        ((VirtualLinkForm)form).setUrl(splittedLink[1]);
			        ((VirtualLinkForm)form).setHostId(host.getIdentifier());
			    }
			}
		}
		
		/*
		 * return to edit page
		 *  
		 */
		
		//If it is a new virtual link then we default to selected host
		if (!UtilMethods.isSet(((VirtualLinkForm) form).getInode()) && !UtilMethods.isSet(((VirtualLinkForm) form).getHostId())) {
			ActionRequestImpl reqImpl = (ActionRequestImpl) req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
			HttpSession session = httpReq.getSession();
	        String hostId= (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	        if(!UtilMethods.isSet(hostId) || hostId.equals("allHosts"))
	        	((VirtualLinkForm)form).setHostId("0");
	        if((UtilMethods.isSet(hostId) && !hostId.equals("allHosts")) && ((VirtualLinkForm)form).getHostId()==null)
	        	((VirtualLinkForm)form).setHostId(hostId);
		}
		setForward(req, "portlet.ext.virtuallinks.edit_virtuallink");
	}


	private void _retrieveVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {

        VirtualLink ml = (VirtualLink) InodeFactory.getInode(req.getParameter("inode"),VirtualLink.class);
        Logger.debug(this, "ML:" + ml.getInode());
        req.setAttribute(WebKeys.VIRTUAL_LINK_EDIT, ml);
	
	}

	private void _saveVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		HostAPI hostAPI = APILocator.getHostAPI();

		VirtualLinkForm mlForm = (VirtualLinkForm) form;
		
		String completeUrl = null;

		String url = mlForm.getUrl();
		if( !url.startsWith("/") ) {
			url = "/" + url;
		}
		
		if (InodeUtils.isSet(mlForm.getHostId())) {
		    Host host = hostAPI.find(mlForm.getHostId(), user, false);
		    completeUrl = host.getHostname() + ":" + url;
		} else {
		    completeUrl = url;
		}
		
		if (completeUrl.trim().endsWith("/")) {
		    completeUrl = completeUrl.trim().substring(0, completeUrl.trim().length() - 1);
		}
		
        VirtualLink vl = null;
        try{
        	vl = VirtualLinkFactory.getVirtualLinkByURL(completeUrl);
        	if(vl == null || !UtilMethods.isSet(vl.getIdentifier())){
        		vl = new VirtualLink();
        		vl.setUrl(completeUrl);
        	}
        }
        catch(DotHibernateException dhe){
        	vl = new VirtualLink();
        	vl.setUrl(completeUrl);
        	Logger.debug(VirtualLinksCache.class, "failed to find: " + completeUrl);  
        }
        
        vl = virtualLinkAPI.checkVirtualLinkForEditPermissions(vl, user);
        
        if(vl == null){
        	SessionMessages.add(req,"message", "message.virtuallink.nopermission.save");
			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
        }
		
		if (vl != null && InodeUtils.isSet(vl.getInode()) && !mlForm.getInode().equalsIgnoreCase(vl.getInode())) {
			//there is another virtual link with the same url... urls are unique
			SessionMessages.add(req,"message", "message.virtuallink.uniquelink.save");
			throw new Exception(WebKeys.UNIQUE_VIRTUAL_LINK_EXCEPTION);

		}
		else {
		
			VirtualLink ml = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);
			//removes this URL from cache
			if (UtilMethods.isSet(ml.getUrl())) {
			    VirtualLinksCache.removePathFromCache(ml.getUrl());				
			}
	
			BeanUtils.copyProperties(ml,form);
				
			ml.setUrl(completeUrl);

			String htmlPageInode = mlForm.getHtmlInode();
			if (InodeUtils.isSet(htmlPageInode)) {
				//it's an internal page
				HTMLPage htmlPage = (HTMLPage) InodeFactory.getInode(htmlPageInode+"",HTMLPage.class);
				Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
				ml.setUri(identifier.getURI());
			}
	
			HibernateUtil.saveOrUpdate(ml);
			SessionMessages.add(req,"message", "message.virtuallink.save");
			
			//reset url to cache
			VirtualLinksCache.removePathFromCache(ml.getUrl());
		    VirtualLinksCache.addPathToCache(ml);

			//VirtualLinkFactory
		}
		//wipe the subscriber list form bean
		BeanUtils.copyProperties(form,req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT));
	}

	private void _deleteVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config, ActionForm form , User user)
	throws Exception {

		VirtualLink vl = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);

		//removes this URL from cache
		if (UtilMethods.isSet(vl.getUrl())) {
		    VirtualLinksCache.removePathFromCache(vl.getUrl());
		}
		
		vl = virtualLinkAPI.checkVirtualLinkForEditPermissions(vl, user);
	        
	    if(vl == null){
	        SessionMessages.add(req,"message", "message.virtuallink.nopermission.save");
			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
	     }
			

		InodeFactory.deleteInode(vl);
		//gets the session object for the messages
		SessionMessages.add(req, "message", "message.virtuallink.delete");
	
	}
}
