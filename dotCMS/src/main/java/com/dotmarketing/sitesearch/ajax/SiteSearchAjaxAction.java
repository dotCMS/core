package com.dotmarketing.sitesearch.ajax;

import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchPublishStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class SiteSearchAjaxAction extends IndexAjaxAction {

public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {




		Map<String, String> map = getURIParams();



		String cmd = map.get("cmd");
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		User user = getUser();





		try {
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("site-search", user)) {
				String userName = map.get("u");
				String password = map.get("p");
				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
                    user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("site-search", user)){
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
			writeError(response, e.getMessage());
		}

	}


	public void createSiteSearchIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException, DotDataException {

		Map<String, String> map = getURIParams();
		int shards = 0;
		String alias="";
		boolean def=false;
		try{
		    if(map.get("shards")!=null)
		        shards = Integer.parseInt(map.get("shards"));
		    alias = UtilMethods.isSet(map.get("indexAlias"))?URLDecoder.decode((String) map.get("indexAlias"), "UTF-8"):
		    	UtilMethods.isSet(map.get("alias"))?URLDecoder.decode((String) map.get("alias"), "UTF-8"):"";
		    if(map.get("default")!=null)
		        def=Boolean.parseBoolean(map.get("default"));
		}
		catch(Exception e){
			Logger.warn(this, e.getMessage(), e);
		}

		String indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + ContentletIndexAPIImpl.timestampFormatter.format(new Date());
		APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName, alias, shards);

		if(def)
		    APILocator.getSiteSearchAPI().activateIndex(indexName);
	}


	public void scheduleJob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {

		Map<String, String[]> map = request.getParameterMap();
	    String date = DateUtil.getCurrentDate();

		SiteSearchConfig config = new SiteSearchConfig();
		for(String key : map.keySet()){
			if(((String[]) map.get(key)).length ==1 && !key.equals("langToIndex")){
				config.put(key,((String[]) map.get(key))[0]);
			}
			else{
				config.put(key,map.get(key));
			}
		}
		String taskName = ((String[]) map.get("QUARTZ_JOB_NAME"))[0];
		String taskPreviousName = map.get("OLD_QUARTZ_JOB_NAME")!=null ? ((String[]) map.get("OLD_QUARTZ_JOB_NAME"))[0] : "";

		if(UtilMethods.isSet(taskPreviousName) && !taskName.equals(taskPreviousName)){
			try {
				if(!config.runNow()){
					APILocator.getSiteSearchAPI().deleteTask(taskPreviousName);
				}
				else{
		            APILocator.getSiteSearchAPI().scheduleTask(config);
		            ActivityLogger.logInfo(getClass(), "Job Created", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
		            AdminLogger.log(getClass(), "Job Created", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
		        }
			} catch (Exception e) {
				Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
				AdminLogger.log(getClass(), "Error Creating Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
				writeError(response, e.getMessage());

			}
		}

		try {
			if(config.runNow()){
				APILocator.getSiteSearchAPI().executeTaskNow(config);
                 ActivityLogger.logInfo(getClass(), "Job Started", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
                 AdminLogger.log(getClass(), "Job Started", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
			}
			else{
				APILocator.getSiteSearchAPI().scheduleTask(config);
				ActivityLogger.logInfo(getClass(), "Job Created", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
	            AdminLogger.log(getClass(), "Job Created", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
			}
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
			writeError(response, e.getMessage());

		}
	}


	public void scheduleJobNow(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		try {
			Map<String, String[]> map = request.getParameterMap();

			SiteSearchConfig config = new SiteSearchConfig();
			for(String key : map.keySet()){
				if(((String[]) map.get(key)).length ==1){
					config.put(key,((String[]) map.get(key))[0]);
				}
				else{
					config.put(key,map.get(key));
				}
			}

			APILocator.getSiteSearchAPI().scheduleTask(config);
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
			writeError(response, e.getMessage());

		}
	}




	public void deleteJob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		String date = DateUtil.getCurrentDate();

		try {
			Map<String, String> map = getURIParams();
			String taskName = URLDecoder.decode(map.get("taskName"), "UTF-8");

			APILocator.getSiteSearchAPI().deleteTask(taskName);

			ActivityLogger.logInfo(getClass(), "Job Deleted", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
            AdminLogger.log(getClass(), "Job Deleted", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
            ActivityLogger.logInfo(getClass(), "Error Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
            AdminLogger.log(getClass(), "Error Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
			writeError(response, e.getMessage());

		}

	}

	public void getIndexName(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> map = getURIParams();
        String indexAlias = map.get("indexAlias");
        String indexName = "";
        if(UtilMethods.isSet(indexAlias) && LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {
            String indexName1=APILocator.getESIndexAPI()
                    .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                    .get(indexAlias);
            if(UtilMethods.isSet(indexName1))
                indexName=indexName1;
        }
        response.getWriter().println(indexName);
    }

	public void getJobProgress(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> map = getURIParams();
        StringBuilder json = new StringBuilder();
        SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
        json.append("[");
        for(ScheduledTask task : ssapi.getTasks()){
            int progress=-1, max=-1;
            if(ssapi.isTaskRunning(task.getJobName())){
                SiteSearchPublishStatus ps = ssapi.getTaskProgress(task.getJobName());
                progress=ps.getCurrentProgress() + ps.getBundleErrors();
                max=ps.getTotalBundleWork();
            }
            json.append("{jobname:'").append(task.getJobName()).append("'")
                .append(",progress:").append(progress).append(",max:").append(max)
                .append("},");
        }
        json.append("]");

        response.setContentType("application/json");
        response.getWriter().println(json.toString());
	}

	@Override
    public void getIndexStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    try {
    	    Map<String, String> map = getURIParams();
    	    String indexName = ESIndexHelper.getInstance().getIndexNameOrAlias(map,"indexName",
					"indexAlias", APILocator.getESIndexAPI());
    	    response.setContentType("text/plain");
            response.getWriter().println(APILocator.getIndiciesAPI().loadIndicies().getSiteSearch().equals(indexName) ? "default" : "inactive");
	    }
	    catch(Exception ex) {
	        throw new RuntimeException(ex);
	    }
    }

	@Override
    public void getNotActiveIndexNames(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    try {
            String defaultIndex=APILocator.getIndiciesAPI().loadIndicies().getSiteSearch();
            List<String> ret=new ArrayList<String>();
            for(String ii : APILocator.getSiteSearchAPI().listIndices())
                if(defaultIndex==null || !defaultIndex.equals(ii))
                    ret.add(ii);
            response.setContentType("text/plain");
            response.getWriter().println(ret);
	    }
	    catch(Exception ex) {
	        throw new RuntimeException(ex);
	    }
    }

	@Override
    public void indexList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            response.setContentType("text/plain");
            response.getWriter().println(APILocator.getSiteSearchAPI().listIndices());
        }
        catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
