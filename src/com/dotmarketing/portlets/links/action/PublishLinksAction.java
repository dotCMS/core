package com.dotmarketing.portlets.links.action;

import java.net.URLDecoder;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.2 $
 *
 */
public class PublishLinksAction extends DotPortletAction {

	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		Logger.debug(this, "Running PublishLinksAction!!!!");

		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		
		try {
			//get the user
			User user = com.liferay.portal.util.PortalUtil.getUser(req);

			_publishLinks(req, user);
			
			if ((referer!=null) && (referer.length()!=0)) {
				_sendToReferral(req, res, referer);
			}
			
			setForward(req, "portlet.ext.links.publish_links");

		}
		catch (Exception e) {
			_handleException(e, req);
		}
	}

	private void _publishLinks(ActionRequest req, User user) throws Exception {
		
		String[] publishInode = req.getParameterValues("publishInode");
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		
		if (publishInode!=null) {
			for (int i=0;i<publishInode.length;i++) {
				Link link = (Link) InodeFactory.getInode(publishInode[i],Link.class);
		
				if (InodeUtils.isSet(link.getInode())) {
					//calls the asset factory edit
					
					try{
						PublishFactory.publishAsset(link,reqImpl.getHttpServletRequest());
						SessionMessages.add(req, "message", "message.link_list.published");
					}catch(WebAssetException wax){
						Logger.error(this, wax.getMessage(),wax);
						SessionMessages.add(req, "error", "message.webasset.published.failed");
					}
				}
			}

		}	
		
	}

}