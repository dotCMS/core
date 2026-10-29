package com.dotmarketing.sitesearch.ajax;

import com.dotcms.content.index.IndexTimestamp;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchPublishStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class SiteSearchAjaxAction extends AjaxAction {

	/**
	 * The commands this endpoint serves, which are the ones the Site Search portlet calls. Any
	 * other command name is answered with a 403.
	 */
	static final Set<String> SUPPORTED_COMMANDS = ImmutableSet.of(
			"createSiteSearchIndex",
			"scheduleJob",
			"deleteJob",
			"getJobProgress");

	/**
	 * Checks that the caller has access to the Site Search portlet and invokes the command named in
	 * the request, as long as it is one of {@link #SUPPORTED_COMMANDS}; any other command is
	 * answered with a 403.
	 *
	 * @param request  the current request
	 * @param response the current response
	 */
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {




		Map<String, String> map = getURIParams();



		String cmd = map.get("cmd");
		java.lang.reflect.Method meth = null;
		final Class<?>[] partypes = new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		User user = getUser();





		try {
			// Check permissions if the user has access to the Site Search portlet
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



			if (cmd != null && !SUPPORTED_COMMANDS.contains(cmd)) {
				Logger.warn(this.getClass(), "Command not supported by this endpoint: " + cmd);
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			meth = SiteSearchAjaxAction.class.getMethod(cmd, partypes);

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = SiteSearchAjaxAction.class.getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(this.getClass(), "Trying to run method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class, "Trying to run method:" + cmd);
			Logger.error(SiteSearchAjaxAction.class, e.getMessage(), e.getCause());
			writeError(response, e.getMessage());
		}

	}


	/**
	 * Default command, run when the request names no command this class declares. It does nothing.
	 */
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;
	}

	/**
	 * Writes an error message to the response, translated for the current user when possible.
	 *
	 * @param response the current response
	 * @param error    the message or language key to write
	 */
	public void writeError(HttpServletResponse response, String error) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), error);
		} catch (Exception e) {
			// fall back to the default company locale below
		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(),
						PublicCompanyFactory.getDefaultCompany().getLocale(), error);
			} catch (Exception e) {
				// fall back to the raw message below
			}
		}
		if (ret == null) {
			ret = error;
		}

		response.getWriter().println("FAILURE: " + ret);
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

		String indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + IndexTimestamp.now();
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

}
