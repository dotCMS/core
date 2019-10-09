package com.dotmarketing.portlets.htmlpageviews.action;

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
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import javax.servlet.jsp.PageContext;

/**
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *  
 */
public class ViewHTMLPageViewsAction extends DotPortletAction {

	protected HostWebAPI hostAPI = WebAPILocator.getHostWebAPI();

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
    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {
        Logger.debug(this, "Running ViewHTMLPagesAction!!!!");

        try {
            //gets the user
            User user = _getUser(req);

            if (req.getWindowState().equals(WindowState.NORMAL)) {
                return mapping.findForward("portlet.ext.htmlpageviews.view");
            } else {
                //Mailing lists
                /** @see com.dotmarketing.portal.struts.DotPortletAction._viewWebAssets * */
                _viewWebAssets(req, user);
                return mapping.findForward("portlet.ext.htmlpageviews.view_htmlpage_views");
            }
        } catch (Exception e) {
            req.setAttribute(PageContext.EXCEPTION, e);
            return mapping.findForward(Constants.COMMON_ERROR);
        }
    }

	/**
	 * Needs to be implemented instead of using parent method because we use
	 * template to search for HTMLPages.
	 * 
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param user
	 * @throws Exception
	 */
    private void _viewWebAssets(RenderRequest req, User user) throws Exception {
    	User systemUser = APILocator.getUserAPI().getSystemUser();
    	
        String uri = null;
        Host host = null;
        if (req.getParameter("htmlpage") != null) {
        	Contentlet contentlet = APILocator.getContentletAPI().find(req.getParameter("htmlpage"), systemUser, false);
        	IHTMLPage myHTMLPage = null;
        	if (contentlet!=null){
        		myHTMLPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
        	}
            uri = APILocator.getIdentifierAPI().find(myHTMLPage).getURI();
			host = hostAPI.findParentHost(myHTMLPage, systemUser, false);
            req.setAttribute("htmlPage", myHTMLPage);
        } else if (req.getParameter("pageIdentifier") != null) {
            //Identifier id = (Identifier) InodeFactory.getInode(req.getParameter("pageIdentifier"), Identifier.class);
        	Identifier id = APILocator.getIdentifierAPI().find(req.getParameter("pageIdentifier"));
            uri = id.getURI();
            IHTMLPage myHTMLPage = (IHTMLPage) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(),false);
			host = hostAPI.findParentHost(myHTMLPage, systemUser, false);
            req.setAttribute("htmlPage", myHTMLPage);
        }
        
        if (req.getParameter("pageURL") != null) {
            uri = req.getParameter("pageURL");
            String[] parts = uri.split(":");            
            if (parts.length > 1) {
                host = hostAPI.findByName(parts[0], systemUser, false);
                uri = parts[1];
            } else {
                host = hostAPI.findDefaultHost(systemUser, false);
            }
                        
            Identifier id = APILocator.getIdentifierAPI().find(host, uri);
            IHTMLPage myHTMLPage = (IHTMLPage) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(),false);
            req.setAttribute("htmlPage", myHTMLPage);
        }
        
        req.setAttribute("uri", uri);        
        Logger.debug(this, "Done with ViewHTMLPageViewsAction");
    }
 
}
