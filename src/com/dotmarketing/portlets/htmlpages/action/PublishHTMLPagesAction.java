package com.dotmarketing.portlets.htmlpages.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

import java.net.URLDecoder;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.3 $
 * 
 */
public class PublishHTMLPagesAction extends DotPortletAction {

	@SuppressWarnings("unchecked")
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		String cmd = req.getParameter("cmd");

		Logger.debug(this, "Running PublishHTMLPagesAction!!!! cmd=" + cmd);

		String referer = req.getParameter("referer");
		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		try {
			// get the user
			User user = _getUser(req);

			if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PREPUBLISH)) {
				_prePublishHTMLPages(req, user);

			}
			java.util.List relatedAssets = (java.util.List) req.getAttribute(WebKeys.HTMLPAGE_RELATED_ASSETS);
			java.util.List relatedWorkflows = (java.util.List) req.getAttribute(WebKeys.HTMLPAGE_RELATED_WORKFLOWS);

			if (((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PUBLISH))
					|| ((relatedAssets == null || relatedAssets.size() == 0) && (relatedWorkflows == null || relatedWorkflows.size() == 0))) {
				_publishHTMLPages(req, user);

				if ((referer != null) && (referer.length() != 0)) {
					_sendToReferral(req, res, referer);
				}
			}

			setForward(req, "portlet.ext.htmlpages.publish_htmlpages");

		} catch (Exception e) {
			_handleException(e, req);
		}
	}

    /**
     * Prepares the Publishing process verifying if the HTMLPage have un-published content and notifying about it
     * to the user.
     *
     * @param req
     * @param user
     * @throws Exception
     */
    @SuppressWarnings( "unchecked" )
    private void _prePublishHTMLPages ( ActionRequest req, User user ) throws Exception {

        String[] publishInode = req.getParameterValues( "publishInode" );

        if ( publishInode == null )
            return;

        // calls the publish factory to get related assets
        java.util.List relatedAssets = new java.util.ArrayList();
        java.util.List relatedWorkflows = new java.util.ArrayList();

        for ( String pageInode : publishInode ) {

            //First lets verify if is a new HTMLPage (as content)
            Contentlet htmlPageContentlet = APILocator.getContentletAPI().find( pageInode, user, false );
            if ( htmlPageContentlet != null && InodeUtils.isSet( htmlPageContentlet.getInode() ) ) {
                HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet( htmlPageContentlet );
                relatedAssets = PublishFactory.getUnpublishedRelatedAssetsForPage( htmlPageAsset, relatedAssets, true, user, false );
            } else {//THIS MUST BE A LEGACY HTML PAGE
                HTMLPage htmlPage = (HTMLPage) InodeFactory.getInode( pageInode, HTMLPage.class );
                if ( htmlPage != null && InodeUtils.isSet( htmlPage.getInode() ) ) {
                    relatedAssets = PublishFactory.getUnpublishedRelatedAssets( htmlPage, relatedAssets, true, user, false );
                }
            }
        }

        req.setAttribute( WebKeys.HTMLPAGE_RELATED_WORKFLOWS, relatedWorkflows );
        req.setAttribute( WebKeys.HTMLPAGE_RELATED_ASSETS, relatedAssets );
    }

    /**
     * Publish a HTMLPage page along with any un-published content it have
     *
     * @param req
     * @param user
     * @throws Exception
     */
    private void _publishHTMLPages ( ActionRequest req, User user ) throws Exception {

        String[] publishInode = req.getParameterValues( "publishInode" );

        if ( publishInode == null )
            return;

        ActionRequestImpl reqImpl = (ActionRequestImpl) req;

        for ( String pageInode : publishInode ) {

            java.util.List relatedAssets = new java.util.ArrayList();

            //First lets verify if is a new HTMLPage (as content)
            Contentlet htmlPageContentlet = APILocator.getContentletAPI().find( pageInode, user, false );
            if ( htmlPageContentlet != null && InodeUtils.isSet( htmlPageContentlet.getInode() ) ) {
                HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet( htmlPageContentlet );
                relatedAssets = PublishFactory.getUnpublishedRelatedAssetsForPage( htmlPageAsset, relatedAssets, true, user, false );

                try {

                    //Publish the content related to this page along with the page
                    PublishFactory.publishHTMLPage( htmlPageAsset, relatedAssets, reqImpl.getHttpServletRequest() );

                    ActivityLogger.logInfo( PublishFactory.class, "Publishing HTMLpage action", "User " + user.getUserId() + " publishing page " + htmlPageAsset.getURI(), HostUtil.hostNameUtil( req, _getUser( req ) ) );
                    SessionMessages.add( reqImpl.getHttpServletRequest(), "message", "message.htmlpage_list.published" );
                } catch ( WebAssetException wax ) {
                    Logger.error( this, wax.getMessage(), wax );
                    SessionMessages.add( reqImpl.getHttpServletRequest(), "error", "message.webasset.published.failed" );
                }
            } else {
                HTMLPage htmlpage = (HTMLPage) InodeFactory.getInode( pageInode, HTMLPage.class );
                if ( InodeUtils.isSet( htmlpage.getInode() ) ) {

                    try {

                        //Publish the content related to this page along with the page
                        PublishFactory.publishAsset( htmlpage, reqImpl.getHttpServletRequest() );

                        ActivityLogger.logInfo( PublishFactory.class, "Publishing HTMLpage action", "User " + user.getUserId() + " publishing page " + htmlpage.getURI(), HostUtil.hostNameUtil( req, _getUser( req ) ) );
                        SessionMessages.add( reqImpl.getHttpServletRequest(), "message", "message.htmlpage_list.published" );
                    } catch ( WebAssetException wax ) {
                        Logger.error( this, wax.getMessage(), wax );
                        SessionMessages.add( reqImpl.getHttpServletRequest(), "error", "message.webasset.published.failed" );
                    }
                }
            }

        }

    }

}