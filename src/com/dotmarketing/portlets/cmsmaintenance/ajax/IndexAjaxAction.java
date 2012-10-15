package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.elasticsearch.action.admin.indices.status.IndexStatus;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
            String aliasToRestore=null;
            File ufile=null;
            for(FileItem it : items) {
               if(it.getFieldName().equalsIgnoreCase("indexToRestore")) {
                   indexToRestore=it.getString().trim();
               }
               else if(it.getFieldName().equalsIgnoreCase("aliasToRestore")) {
                   aliasToRestore=it.getString().trim();
               }
               else if(it.getFieldName().equalsIgnoreCase("uploadedfiles[]")) {
                   ufile=File.createTempFile("indexToRestore", "idx");
                   InputStream in=it.getInputStream();
                   FileOutputStream out = new FileOutputStream(ufile);
                   IOUtils.copyLarge(in, out);
                   IOUtils.closeQuietly(out);
                   IOUtils.closeQuietly(in);
               }
               else if(it.getFieldName().equalsIgnoreCase("clearBeforeRestore")) {
                   clearBeforeRestore=true;
               }
            }

            if(LicenseUtil.getLevel()>=200) {
                if(UtilMethods.isSet(aliasToRestore)) {
                    String indexName=APILocator.getESIndexAPI()
                             .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                             .get(aliasToRestore);
                    if(UtilMethods.isSet(indexName))
                        indexToRestore=indexName;
                }
                else if(!UtilMethods.isSet(indexToRestore)) {
                    indexToRestore=APILocator.getIndiciesAPI().loadIndicies().site_search;
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

	protected String getIndexNameOrAlias(Map<String, String> map) {
	    String indexName = map.get("indexName");
	    String indexAlias = map.get("indexAlias");
        if(UtilMethods.isSet(indexAlias) && LicenseUtil.getLevel()>=200) {
            String indexName1=APILocator.getESIndexAPI()
                    .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                    .get(indexAlias);
            if(UtilMethods.isSet(indexName1))
                indexName=indexName1;
        }
        return indexName;
	}

	public void downloadIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		response.setContentType("application/zip");

		String indexName = getIndexNameOrAlias(map);

		if(!UtilMethods.isSet(indexName))return;

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

	public void clearIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotStateException, DotDataException {
		Map<String, String> map = getURIParams();

		String indexName = getIndexNameOrAlias(map);

		if(UtilMethods.isSet(indexName))
		    APILocator.getESIndexAPI().clearIndex(indexName);

	}

	public void deleteIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String indexName = getIndexNameOrAlias(map);
		if(UtilMethods.isSet(indexName))
		    APILocator.getESIndexAPI().delete(indexName);
	}

	public void activateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = getIndexNameOrAlias(map);

		if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
			APILocator.getSiteSearchAPI().activateIndex(indexName);
		}
		else{
			APILocator.getContentletIndexAPI().activateIndex(indexName);
		}

	}
	public void deactivateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = getIndexNameOrAlias(map);
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
		String indexName = getIndexNameOrAlias(map);

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

	public void closeIndex(HttpServletRequest request, HttpServletResponse response) {
	    Map<String, String> map = getURIParams();
	    String indexName = getIndexNameOrAlias(map);

	    APILocator.getESIndexAPI().closeIndex(indexName);
	}

	public void openIndex(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		APILocator.getESIndexAPI().openIndex(indexName);
	}

	public void getActiveIndex(HttpServletRequest request, HttpServletResponse response) throws IOException  {
		Map<String, String> map = getURIParams();
		String type =map.get("type");
		String resp = null;

		try {
			resp =  APILocator.getContentletIndexAPI().getActiveIndexName(type);
		} catch (DotDataException e) {
			resp =  e.getMessage();
		}

		response.getWriter().println(resp);
	}

	public void getIndexStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, String> map = getURIParams();
		String indexName = getIndexNameOrAlias(map);
		String resp = null;

		try {
			resp = APILocator.getESIndexAPI().getIndexStatus(indexName).getStatus();
		} catch (DotDataException e) {
			resp = e.getMessage();
		}

		response.getWriter().println(resp);
    }

	public void getIndexRecordCount(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, String> map = getURIParams();
		String indexName = getIndexNameOrAlias(map);
		ESIndexAPI esapi = APILocator.getESIndexAPI();
		Map<String, IndexStatus> indexInfo = esapi.getIndicesAndStatus();
		IndexStatus status = indexInfo.get(indexName);
		response.getWriter().println((status !=null && status.getDocs() != null) ? status.getDocs().numDocs(): 0);
	}

	public void getNotActiveIndexNames(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
		List<String> indices = idxApi.listDotCMSIndices();
		List<String> inactives = new ArrayList<String>();

		for (String index : indices) {
			try {
				if(APILocator.getESIndexAPI().getIndexStatus(index) == ESIndexAPI.Status.INACTIVE) {
					inactives.add(index);
				}
			} catch (DotDataException e) {
			}
		}

		response.getWriter().println(inactives);
	}

	public void stopReindexThread(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ReindexThread.stopThread();
	}

	public void startReindexThread(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ReindexThread.startThread(Config.getIntProperty("REINDEX_THREAD_SLEEP", 500), Config.getIntProperty("REINDEX_THREAD_INIT_DELAY", 5000));
	}

	public void getReindexThreadStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.getWriter().println(ReindexThread.getInstance().isWorking()?"active":"stopped");
	}

	public void indexList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
        response.getWriter().println(idxApi.listDotCMSIndices());        
    }
}
