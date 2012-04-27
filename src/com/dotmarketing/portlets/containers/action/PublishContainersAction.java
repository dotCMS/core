package com.dotmarketing.portlets.containers.action;

import java.net.URLDecoder;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
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
public class PublishContainersAction extends DotPortletAction {

	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		
		Logger.debug(this, "Running PublishContainersAction!!!!");

		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		
		try {
			//get the user
			User user = com.liferay.portal.util.PortalUtil.getUser(req);

			_publishContainers(req, user);
			
			if ((referer!=null) && (referer.length()!=0)) {
				_sendToReferral(req, res, referer);
			}
			
			setForward(req, "portlet.ext.containers.publish_containers");

		}
		catch (Exception e) {
			_handleException(e, req);
		}
	}

	private void _publishContainers(ActionRequest req, User user) throws WebAssetException, DotDataException, DotSecurityException {
		
		String[] publishInode = req.getParameterValues("publishInode");
		if (publishInode == null) return;
		
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		
		for (int i=0;i<publishInode.length;i++) {
			Container container = (Container) InodeFactory.getInode(publishInode[i],Container.class);
	
			if (InodeUtils.isSet(container.getInode())) {
				//calls the asset factory edit
				
				try{
					PublishFactory.publishAsset(container,reqImpl.getHttpServletRequest());
					SessionMessages.add(req, "message", "message.container_list.published");
				} catch(DotSecurityException wax){
					Logger.error(this, wax.getMessage(),wax);
					SessionMessages.add(req, "error", "message.webasset.published.failed");
				} catch(WebAssetException wax){
					Logger.error(this, wax.getMessage(),wax);
					SessionMessages.add(req, "error", "message.webasset.published.failed");
				}
			}
			
			ActivityLogger.logInfo(this.getClass(), "Publishing Container action", "User " + user.getPrimaryKey() + " publishing container" + container.getTitle(), HostUtil.hostNameUtil(req, user));
		}

		
		
	}

}