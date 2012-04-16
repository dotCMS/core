package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class IndexAjaxAction extends AjaxAction {

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		
		
		Map<String, String> map = getURIParams();
		
		
		
		String cmd = map.get("cmd");
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		User user = getUser();
		
		
		
		
		
		try {
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
				String userName = map.get("u");
				String password = map.get("p");
				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
                    user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
					response.sendError(401);
					return;
				}
			}

			
			
			meth = this.getClass().getMethod(cmd, partypes);

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = this.getClass().getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(this.getClass(), "Trying to run method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(IndexAjaxAction.class, "Trying to run method:" + cmd);
			Logger.error(IndexAjaxAction.class, e.getMessage(), e.getCause());
			return;
		}

	}
	
	public void restoreIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
	    try {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
            
            String indexToRestore=null;
            boolean clearBeforeRestore=false;
            File ufile=null;
            for(FileItem it : items) {
               if(it.getFieldName().equals("indexToRestore")) {
                   indexToRestore=it.getString().trim();
               }
               else if(it.getFieldName().equals("uploadedfiles[]")) {
                   ufile=File.createTempFile("indexToRestore", "idx");
                   InputStream in=it.getInputStream();
                   FileOutputStream out = new FileOutputStream(ufile);
                   IOUtils.copyLarge(in, out);
                   IOUtils.closeQuietly(out);
                   IOUtils.closeQuietly(in);
               }
               else if(it.getFieldName().equals("clearBeforeRestore")) {
                   clearBeforeRestore=true;
               }
            }
            
            if(ufile!=null) {
                final boolean clear=clearBeforeRestore;
                final String index=indexToRestore;
                final File file=ufile;
                new Thread() {
                    public void run() {
                        try {
                            if(clear)
                            	APILocator.getESIndexAPI().clearIndex(index);
                            APILocator.getESIndexAPI().restoreIndex(file, index);
                            Logger.info(this, "finished restoring index "+index);
                        }
                        catch(Exception ex) {
                            Logger.error(IndexAjaxAction.this, "Error restoring",ex);
                        }
                    }
                }.start();
            }
	    }
	    catch(FileUploadException fue) {
	        Logger.error(this, "Error uploading file", fue);
	        throw new IOException(fue);
	    }
	}


	public void downloadIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		response.setContentType("application/zip");
		String indexName = map.get("indexName");
		
		if(indexName == null)return;
		
		if(indexName.equalsIgnoreCase("live") || indexName.equalsIgnoreCase("working")){
			IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
			if(indexName.equalsIgnoreCase("live")){
				indexName = info.live;
			}
			if(indexName.equalsIgnoreCase("working")){
				indexName = info.working;
			}
		}
		
		File f = APILocator.getESIndexAPI().backupIndex(indexName);
		response.setContentLength((int) f.length());
		OutputStream out = response.getOutputStream();
		InputStream in = new FileInputStream(f);
		
		response.setHeader("Content-Type", "application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=" + indexName + ".zip");
		
		IOUtils.copyLarge(in, out);
		
		f.delete();
		return;
	}
	
	

	
	
	
	
	public void createIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		
		Map<String, String> map = getURIParams();
		int shards = 0;
		
		try{
			shards = Integer.parseInt(map.get("shards"));

		}
		catch(Exception e){
			
		}
		

		boolean live = map.get("live") != null;
		String indexName = map.get("indexName");
		if(indexName == null)
		    indexName=ESContentletIndexAPI.timestampFormatter.format(new Date());
		indexName = (live) ? "live_" + indexName : "working_" + indexName; 
		APILocator.getContentletIndexAPI().createContentIndex(indexName, shards);

	}
	
	public void clearIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		if(indexName == null)return;

		APILocator.getESIndexAPI().clearIndex(indexName);
		
		
	}
	
	public void deleteIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		if(indexName == null)return;

		APILocator.getESIndexAPI().delete(indexName);
		
		
	}
	
	public void activateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		if(indexName == null)return;
		if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
			APILocator.getSiteSearchAPI().activateIndex(indexName);
		}
		else{
			APILocator.getContentletIndexAPI().activateIndex(indexName);
		}

	}
	public void deactivateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		if(indexName == null)return;
		if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
			APILocator.getSiteSearchAPI().deactivateIndex(indexName);
		}
		else{
			
			APILocator.getContentletIndexAPI().deactivateIndex(indexName);
		}
	}
	
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;
		
	}
	
	
	public void updateReplicas(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		int replicas = Integer.parseInt(map.get("replicas"));

		
		APILocator.getESIndexAPI().updateReplicas(indexName, replicas);

	}
	
	
	public void writeError(HttpServletResponse response, String error) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), error);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						error);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = error;
		}

		response.getWriter().println("FAILURE: " + ret);
	}

	
	
	
	
	
}
