package com.dotmarketing.portlets.files.action;

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
import com.dotmarketing.portlets.files.model.File;
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
public class PublishFilesAction extends DotPortletAction {

	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

        Logger.debug(this, "Running PublishFilesAction!!!!");

		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		
		try {
			//get the user
			User user = _getUser(req);

			_publishFiles(req, user);
			
			if ((referer!=null) && (referer.length()!=0)) {
				_sendToReferral(req, res, referer);
			}
			
			setForward(req, "portlet.ext.files.publish_files");

		}
		catch (Exception e) {
			_handleException(e, req);
		}
	}

	private void _publishFiles(ActionRequest req, User user) throws WebAssetException {
		
		String[] publishInode = req.getParameterValues("publishInode");
		if (publishInode == null) return;
		
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		
		for (int i=0;i<publishInode.length;i++) {
			File file = (File) InodeFactory.getInode(publishInode[i],File.class);
	
			if (InodeUtils.isSet(file.getInode())) {
				//calls the asset factory edit
				
				try{
					PublishFactory.publishAsset(file,reqImpl.getHttpServletRequest());
					SessionMessages.add(req, "message", "message.file_list.published");
				} catch(Exception wax){
					Logger.error(this, wax.getMessage(),wax);
					SessionMessages.add(req, "error", "message.webasset.published.failed");
				}
			}
		}
		
	}

}