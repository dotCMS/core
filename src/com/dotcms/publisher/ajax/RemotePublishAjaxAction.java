package com.dotcms.publisher.ajax;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class RemotePublishAjaxAction extends AjaxAction {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		return;
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String cmd = map.get("cmd");
		Method dispatchMethod = null;
		if(null!=cmd){
			try {
				dispatchMethod = this.getClass().getMethod(cmd, new Class[]{HttpServletRequest.class, HttpServletResponse.class});
			} catch (Exception e) {
				try {
					dispatchMethod = this.getClass().getMethod("action", new Class[]{HttpServletRequest.class, HttpServletResponse.class});
				} catch (Exception e1) {
					Logger.error(this.getClass(), "Trying to get method:" + cmd);
					Logger.error(this.getClass(), e1.getMessage(), e1.getCause());
					throw new DotRuntimeException(e1.getMessage());
				}
			} 			
			try {
				dispatchMethod.invoke(this, new Object[]{request,response});
			} catch (Exception e) {
				Logger.error(this.getClass(), "Trying to invoke method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				throw new DotRuntimeException(e.getMessage());
			}			
		}
	}
	
	public void publish(HttpServletRequest request, HttpServletResponse response) 		
		throws WorkflowActionFailureException {
			try {
				PublisherAPI publisherAPI = PublisherAPI.getInstance();
				String _assetId = request.getParameter("assetIdentifier");
				String _contentPushPublishDate = request.getParameter("remotePublishDate");
				String _contentPushPublishTime = request.getParameter("remotePublishTime");
				String _contentPushExpireDate = request.getParameter("remotePublishExpireDate");
				String _contentPushExpireTime = request.getParameter("remotePublishExpireTime");
				boolean _contentPushNeverExpire = "on".equals(request.getParameter("remotePublishNeverExpire"))?true:false;

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-H-m");
				Date publishDate = dateFormat.parse(_contentPushPublishDate+"-"+_contentPushPublishTime);
				
				List<String> ids = new ArrayList<String>();			

				try {
					
					// if the asset is a folder put the inode instead of the identifier
					Folder folder = null;
					try {
						folder = APILocator.getFolderAPI().find(_assetId, getUser(), false);
					} catch(DotDataException e) {
					}
					
					if(UtilMethods.isSet(folder)) {
						ids.add(_assetId);
					} else {
						// if the asset is not a folder and has identifier, put it, if not, put the inode
						Identifier iden = APILocator.getIdentifierAPI().findFromInode(_assetId);
						ids.add(iden.getId());
					}
				} catch(DotStateException e) {
					ids.add(_assetId);
				} catch (DotSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String bundleId = UUID.randomUUID().toString();			
				
				publisherAPI.addContentsToPublish(ids, bundleId, publishDate, getUser());
				if(!_contentPushNeverExpire && (!"".equals(_contentPushExpireDate.trim()) && !"".equals(_contentPushExpireTime.trim()))){
					bundleId = UUID.randomUUID().toString();
					Date expireDate = dateFormat.parse(_contentPushExpireDate+"-"+_contentPushExpireTime);
					publisherAPI.addContentsToUnpublish(ids, bundleId, expireDate);
				}
			} catch (DotPublisherException e) {
				Logger.debug(PushPublishActionlet.class, e.getMessage());
				throw new  WorkflowActionFailureException(e.getMessage());
			} catch (ParseException e){
				Logger.debug(PushPublishActionlet.class, e.getMessage());
				throw new  WorkflowActionFailureException(e.getMessage());			
			} catch (DotDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}
		
}
