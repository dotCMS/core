package com.dotmarketing.portlets.htmlpageviews.action;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpageviews.factories.HTMLPageViewFactory;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"> <b><i>View Source </i> </b> </a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.5 $
 *  
 */
public class ViewHTMLPageViewsAction extends DotPortletAction {

	protected HostWebAPI hostAPI = WebAPILocator.getHostWebAPI();

	/*
     * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.portlet.PortletConfig,
     *      javax.portlet.RenderRequest, javax.portlet.RenderResponse)
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
                List<MailingList> list = MailingListFactory.getMailingListsByUser(user);
                list.add(MailingListFactory.getUnsubscribersMailingList());
                req.setAttribute(WebKeys.MAILING_LIST_VIEW, list);

                /** @see com.dotmarketing.portal.struts.DotPortletAction._viewWebAssets * */
                _viewWebAssets(req, user);
                return mapping.findForward("portlet.ext.htmlpageviews.view_htmlpage_views");
            }
        } catch (Exception e) {
            req.setAttribute(PageContext.EXCEPTION, e);
            return mapping.findForward(Constants.COMMON_ERROR);
        }
    }

 

    //Needs to be implemented instead of using parent method because we use
    // template to search for HTMLPages
    private void _viewWebAssets(RenderRequest req, User user) throws Exception {

    	User systemUser = APILocator.getUserAPI().getSystemUser();
    	
        String uri = null;
        Host host = null;
        if (req.getParameter("htmlpage") != null) {
            HTMLPage myHTMLPage = (HTMLPage) InodeFactory.getInode(req.getParameter("htmlpage"), HTMLPage.class);
            uri = APILocator.getIdentifierAPI().find(myHTMLPage).getURI();
			host = hostAPI.findParentHost(myHTMLPage, systemUser, false);
            req.setAttribute("htmlPage", myHTMLPage);
        } else if (req.getParameter("pageIdentifier") != null) {
            //Identifier id = (Identifier) InodeFactory.getInode(req.getParameter("pageIdentifier"), Identifier.class);
        	Identifier id = APILocator.getIdentifierAPI().find(req.getParameter("pageIdentifier"));
            uri = id.getURI();
            HTMLPage myHTMLPage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(),false);
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
            HTMLPage myHTMLPage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(),false);
            req.setAttribute("htmlPage", myHTMLPage);
            if (!InodeUtils.isSet(id.getInode())) {

                VirtualLink vl = null;
                try{
                	vl = VirtualLinkFactory.getVirtualLinkByURL(uri);
                }
                catch(DotHibernateException dhe){
                	Logger.debug(VirtualLinksCache.class, "failed to find: " + uri);  
                }
                if (vl != null && !InodeUtils.isSet(vl.getInode())) {

                    myHTMLPage.setTitle(LanguageUtil.get(user, "message.htmlpageviews.pagenotfound"));
                    SessionMessages.add(req, "message", "message.htmlpageviews.pagenotfound");
                } else {
                    req.setAttribute(WebKeys.VIRTUAL_LINK_EDIT, vl);
                }
            }
        }
        
        req.setAttribute("uri", uri);        
 
        Logger.debug(this, "Done with ViewHTMLPageViewsAction");

    }

 
}