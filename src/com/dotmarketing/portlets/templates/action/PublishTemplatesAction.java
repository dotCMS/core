package com.dotmarketing.portlets.templates.action;

import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.3 $
 *
 */
public class PublishTemplatesAction extends DotPortletAction {

	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

        Logger.debug(this, "Running PublishTemplatesAction!!!!");

		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		
		try {
			//get the user
			User user = com.liferay.portal.util.PortalUtil.getUser(req);

			_publishTemplates(req, user);
			
			if ((referer!=null) && (referer.length()!=0)) {
				_sendToReferral(req, res, referer);
			}
			
			setForward(req, "portlet.ext.templates.publish_templates");

		}
		catch (Exception e) {
			_handleException(e, req);
		}
	}

	@SuppressWarnings("unchecked")
	private void _publishTemplates(ActionRequest req, User user) throws Exception {
		
		String[] publishInode = req.getParameterValues("publishInode");

		if (publishInode == null) return;
		
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;

		//creates a set of related pages for this template
		Set relatedPages = new HashSet();

		for (int i=0;i<publishInode.length;i++) {

			Template template = (Template) InodeFactory.getInode(publishInode[i],Template.class);
			
			if (InodeUtils.isSet(template.getInode())) {
	        	
				//calls the asset factory edit
				try{
					PublishFactory.publishAsset(template,reqImpl.getHttpServletRequest());
					SessionMessages.add(req, "message", "message.template_list.published");
				}catch(WebAssetException wax){
					Logger.error(this, wax.getMessage(),wax);
					SessionMessages.add(req, "error", "message.webasset.published.failed");
				}
				List<HTMLPage> pagesForThisTemplate = APILocator.getTemplateAPI().getPagesUsingTemplate(template, user, false);
				relatedPages.addAll(pagesForThisTemplate);
			}
		}
		
		Iterator iterPages = relatedPages.iterator();
		//iterates through the children pages
		while (iterPages.hasNext()) {
			//writes the page to a file
			PageServices.invalidate((HTMLPage) iterPages.next());
		}
		
		
		
	}

}