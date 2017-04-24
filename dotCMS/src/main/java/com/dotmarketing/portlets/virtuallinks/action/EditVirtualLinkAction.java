package com.dotmarketing.portlets.virtuallinks.action;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
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
 * This Struts action allows users to edit Vanity URLs (a.k.a. Virtual Links) in
 * dotCMS.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class EditVirtualLinkAction extends DotPortletAction {
	
	private static VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI(); 

	/**
	 * The main entry point that will handle all requests to the Struts action.
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
	 *             An error occurred when editing a Vanity URL.
	 */
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
	    
		// Get the mailing list object, stick it in request
		try {
	        Logger.debug(this, "I'm retrieving the list");
			_retrieveVirtualLink(req, res, config, form);
		}
		catch (Exception ae) {
			_handleException(ae, req);
		}

		// If we are saving... 
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
		// If we are deleting...
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
		// If we are editing...
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
		    VirtualLink vl = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);
		    // Copy props from the db to the form bean
			BeanUtils.copyProperties(form, req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT));
			String linkUrl = vl.getUrl();
			if (linkUrl != null) {
			    String[] splittedLink = linkUrl.split(":");
			    if (splittedLink.length > 1) {
			        Host host = hostAPI.findByName(splittedLink[0], user, false);
                    if ( host != null ) {
                        ((VirtualLinkForm) form).setUrl( splittedLink[1] );
                        ((VirtualLinkForm) form).setHostId( host.getIdentifier() );
                    } else {
                        Logger.error( this, "Host not found OR Unexpected URL format for Vanity URL: " + linkUrl );
                    }
                }
			}
		}
		// Return to edit page
		// If it is a new virtual link then we default to selected host
		if (!UtilMethods.isSet(((VirtualLinkForm) form).getInode()) && !UtilMethods.isSet(((VirtualLinkForm) form).getHostId())) {
			ActionRequestImpl reqImpl = (ActionRequestImpl) req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
			HttpSession session = httpReq.getSession();
	        String hostId= (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	        if(!UtilMethods.isSet(hostId) || hostId.equals("allHosts")) {
	        	((VirtualLinkForm)form).setHostId("0");
	        }
	        if((UtilMethods.isSet(hostId) && !hostId.equals("allHosts")) && ((VirtualLinkForm)form).getHostId()==null) {
	        	((VirtualLinkForm)form).setHostId(hostId);
	        }
		}
		setForward(req, "portlet.ext.virtuallinks.edit_virtuallink");
	}

	/**
	 * Retrieves the {@link VirtualLink} object from the specified Inode in the
	 * request.
	 * 
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param res
	 *            - The HTTP Response wrapper.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @throws Exception
	 *             An error occurred when retrieving the Vanity URL.
	 */
	private void _retrieveVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {
        VirtualLink ml = (VirtualLink) InodeFactory.getInode(req.getParameter("inode"),VirtualLink.class);
        Logger.debug(this, "ML: " + ml.getInode());
        req.setAttribute(WebKeys.VIRTUAL_LINK_EDIT, ml);
	}

	/**
	 * Saves the Vanity URL. The specified user must have the appropriate
	 * permissions on the site in order to interact with Vanity URLs.
	 * 
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param res
	 *            - The HTTP Response wrapper.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 *             An error occurred when saving the Vanity URL.
	 */
	private void _saveVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		final VirtualLinkForm mlForm = (VirtualLinkForm) form;
		final Host site = hostAPI.find(mlForm.getHostId(), user, false);
		final String uri;
		String htmlPageInode = mlForm.getHtmlInode();
		if (InodeUtils.isSet(htmlPageInode)) {
			// It's an internal page
			final IHTMLPage htmlPage = (IHTMLPage) InodeFactory.getInode(htmlPageInode, IHTMLPage.class);
			final Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
			uri = identifier.getURI();
		} else {
			uri = mlForm.getUri();
		}
		VirtualLink vanityUrl = virtualLinkAPI.create(mlForm.getTitle(), mlForm.getUrl(), uri, mlForm.isActive(), site,
				user);
		if (UtilMethods.isSet(req.getParameter("inode"))) {
			// Updating an existing Vanity URL
			vanityUrl = getVanityUrlFromRequest(req, form, vanityUrl.getUrl(), vanityUrl.getUri());
		}
		try {
			virtualLinkAPI.save(vanityUrl, user);
		} catch (DotSecurityException e) {
			SessionMessages.add(req, "message", "message.virtuallink.nopermission.save");
			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
		} catch (DotDuplicateDataException e) {
			SessionMessages.add(req, "message", "message.virtuallink.uniquelink.save");
			throw new Exception(WebKeys.UNIQUE_VIRTUAL_LINK_EXCEPTION);
		} catch (DotHibernateException dhe) {
			Logger.debug(VirtualLinksCache.class, "Failed to find Vanity URL with URL: " + vanityUrl.getUrl());
		}
		SessionMessages.add(req, "message", "message.virtuallink.save");
		req.setAttribute(WebKeys.VIRTUAL_LINK_EDIT, vanityUrl);
		BeanUtils.copyProperties(form, req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT));
	}

	/**
	 * Deletes the Vanity URL. The specified user must have the appropriate
	 * permissions on the site in order to interact with Vanity URLs.
	 * 
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param res
	 *            - The HTTP Response wrapper.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws Exception
	 *             An error occurred when deleting the Vanity URL.
	 */
	private void _deleteVirtualLink(ActionRequest req, ActionResponse res,PortletConfig config, ActionForm form , User user)
	throws Exception {
		final VirtualLink vanityUrl = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);
		try {
			virtualLinkAPI.delete(vanityUrl, user);
		} catch (DotSecurityException e) {
			SessionMessages.add(req,"message", "message.virtuallink.nopermission.save");
			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		// Gets the session object for the messages
		SessionMessages.add(req, "message", "message.virtuallink.delete");
	}

	/**
	 * Retrieves the Vanity URL object form the request in case an update is
	 * being done. This is necessary in order to avoid exceptions related to the
	 * object memory reference in Hibernate.
	 * 
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param completeUrl
	 *            - The complete URL.
	 * @param uri
	 *            - The internal URI to a page in dotCMS.
	 * @return The valid Hibernate reference to the {@link VirtualLink} object.
	 * @throws IllegalAccessException
	 *             Form properties could not be copied via the {@link BeanUtils}
	 *             class.
	 * @throws InvocationTargetException
	 *             Form properties could not be copied via the {@link BeanUtils}
	 *             class.
	 */
	protected VirtualLink getVanityUrlFromRequest(ActionRequest req, ActionForm form, String completeUrl, String uri)
			throws IllegalAccessException, InvocationTargetException {
		VirtualLink vanityUrl = (VirtualLink) req.getAttribute(WebKeys.VIRTUAL_LINK_EDIT);
		BeanUtils.copyProperties(vanityUrl, form);
		vanityUrl.setUrl(completeUrl);
		vanityUrl.setUri(uri);
		return vanityUrl;
	}

}
