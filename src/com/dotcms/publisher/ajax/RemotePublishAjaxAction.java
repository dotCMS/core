package com.dotcms.publisher.ajax;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.hadoop.mapred.lib.Arrays;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.PublishThread;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

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
		
		User user = getUser();
		
		try{
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
				String userName = map.get("u") !=null 
					? map.get("u") 
						: map.get("user") !=null 
							? map.get("user") 
								: null;
			
				String password = map.get("p") !=null 
					? map.get("p") 
							: map.get("passwd") !=null 
								? map.get("passwd") 
									: null;
			

				
				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
	                user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", user)){
					response.sendError(401);
					return;
				}
			}
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage());
			response.sendError(401);
			return;
		}
		
		
		
		
		
		
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
	
	public void unPublish(HttpServletRequest request, HttpServletResponse response) throws DotPublisherException {
	    PublisherAPI publisherAPI = PublisherAPI.getInstance();
        String assetId = request.getParameter("assetIdentifier");
        List<String> identifiers=new ArrayList<String>();
        if(assetId.contains(","))
            identifiers.addAll(Arrays.asList(assetId.split(",")));
        else
            identifiers.add(assetId);
        
        publisherAPI.addContentsToUnpublish(identifiers, UUIDGenerator.generateUuid(), new Date(), getUser());
	}
	
	public void publish(HttpServletRequest request, HttpServletResponse response) throws WorkflowActionFailureException {
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
					
					if(folder!=null && UtilMethods.isSet(folder.getInode())) {
						ids.add(_assetId);
					} else {
						// if the asset is not a folder and has identifier, put it, if not, put the inode
						Identifier iden = APILocator.getIdentifierAPI().findFromInode(_assetId);
						ids.add(iden.getId());
					}
				} catch(DotStateException e) {
					ids.add(_assetId);
				} catch (DotSecurityException e) {
					e.printStackTrace();
				}
				
				String bundleId = UUID.randomUUID().toString();			
				
				publisherAPI.addContentsToPublish(ids, bundleId, publishDate, getUser());
				if(!_contentPushNeverExpire && (!"".equals(_contentPushExpireDate.trim()) && !"".equals(_contentPushExpireTime.trim()))){
					bundleId = UUID.randomUUID().toString();
					Date expireDate = dateFormat.parse(_contentPushExpireDate+"-"+_contentPushExpireTime);
					publisherAPI.addContentsToUnpublish(ids, bundleId, expireDate, getUser());
				}
			} catch (DotPublisherException e) {
				Logger.debug(PushPublishActionlet.class, e.getMessage());
				throw new  WorkflowActionFailureException(e.getMessage());
			} catch (ParseException e){
				Logger.debug(PushPublishActionlet.class, e.getMessage());
				throw new  WorkflowActionFailureException(e.getMessage());			
			} catch (DotDataException e) {
				Logger.error(PushPublishActionlet.class, e.getMessage(), e);
			}	
	}
	
	
	public void downloadBundle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		response.setContentType("application/x-tgz");
		
		String bid = map.get("bid");
		PublisherConfig config = new PublisherConfig();
		config.setId(bid);
		File bundleRoot = BundlerUtil.getBundleRoot(config);

		ArrayList<File> list = new ArrayList<File>(1);
		list.add(bundleRoot);
		File bundle = new File(bundleRoot+File.separator+".."+File.separator+config.getId()+".tar.gz");
		if(!bundle.exists()){
			response.sendError(500, "No Bundle Found");
			return;
		}
		
		response.setHeader("Content-Disposition", "attachment; filename=" + config.getId()+".tar.gz");
		BufferedInputStream in = null;
		try{
			in = new BufferedInputStream(new FileInputStream(bundle));
			byte[] buf = new byte[4096]; 
			int len;
	
			while ((len = in.read(buf, 0, buf.length))!= -1){
				response.getOutputStream().write(buf, 0, len);
			}
		}
		catch(Exception e){
			
		}
		finally{
			try{
				in.close();
			}
			catch(Exception ex){};
		}
		return;
	}
	
	public void uploadBundle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException, FileUploadException {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        @SuppressWarnings("unchecked")
		List<FileItem> items = (List<FileItem>) upload.parseRequest(request);

		InputStream bundle = items.get(0).getInputStream();
		String bundleName = items.get(0).getName();
		String bundlePath = ConfigUtils.getBundlePath()+File.separator;
		String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
		String endpointId = getUser().getUserId();
		response.setContentType("text/html; charset=utf-8");  
		PrintWriter out = response.getWriter();  
		
		PublishAuditStatus status;
		try {
			status = PublishAuditAPI.getInstance().updateAuditTable(endpointId, null, bundleFolder);
		
	//		Write file on FS
			FileUtil.writeToFile(bundle, bundlePath+bundleName);

			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				new Thread(new PublishThread(bundleName, null, endpointId, status)).start();
			}

			out.print("<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'success'}</textarea></body></html>");
		
		} catch (DotPublisherException e) {
			// TODO Auto-generated catch block
			out.print("<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'error'}</textarea></body></html>");
		}
		
	}
}
