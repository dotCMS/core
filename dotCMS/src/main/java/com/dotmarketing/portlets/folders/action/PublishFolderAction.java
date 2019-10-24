package com.dotmarketing.portlets.folders.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderAPIImpl;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Maria
 */

public class PublishFolderAction extends DotPortletAction {

	public static boolean debug = false;

	@SuppressWarnings("rawtypes")
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		try {
			String cmd = req.getParameter("cmd");

			if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PREPUBLISH)) {

				//prepublish
				_prePublishFolder(req,res,config,form);

			}
			java.util.List relatedAssets = (java.util.List) req.getAttribute(WebKeys.FOLDER_RELATED_ASSETS);

			if (((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PUBLISH)) ||
				(relatedAssets.size()==0)) {

				_publishFolder(req,res,config,form);

		        String referer = URLDecoder.decode(req.getParameter("referer"),"UTF-8");

		        //Waits for reindex to load page with latest contentlets status (live/working)
                ThreadUtils.sleep(1000);

		        _sendToReferral(req,res,referer + "&p_p_state="+ WindowState.MAXIMIZED + "&p_p_id=site-browser&p_p_action=1");
		        return;
			}
			setForward(req,"portlet.ext.folders.publish_folder");

        } catch (Exception ae) {
        	_handleException(ae,req);
        }

    }

	@SuppressWarnings({ "rawtypes" })
	private void _prePublishFolder(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {


		String inode = req.getParameter("inode");

		FolderAPI folderAPI = new FolderAPIImpl();
		Folder folder = folderAPI.find(inode, _getUser(req), false);

		java.util.List relatedAssets = new java.util.ArrayList();

        if (InodeUtils.isSet(folder.getInode())) {
			//calls the asset factory edit
			relatedAssets = PublishFactory.getUnpublishedRelatedAssets(folder,relatedAssets, _getUser(req), false);
        }
		req.setAttribute(WebKeys.FOLDER_RELATED_ASSETS,relatedAssets);
	}

	private void _publishFolder(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {

		String inode = req.getParameter("inode");

		FolderAPI folderAPI = new FolderAPIImpl();
		Folder folder = folderAPI.find(inode, _getUser(req), false);

		ActionRequestImpl areq = (ActionRequestImpl) req;
		HttpServletRequest hreq = areq.getHttpServletRequest();

		if (InodeUtils.isSet(folder.getInode())) {
			try{
				PublishFactory.publishAsset(folder,hreq);
				SessionMessages.add(req, "message", "message.folder.published");
			}catch (WebAssetException wax) {
				Logger.error(this, wax.getMessage(),wax);
				SessionMessages.add(req, "error", "message.webasset.published.failed");

			}
		}

	}

}
