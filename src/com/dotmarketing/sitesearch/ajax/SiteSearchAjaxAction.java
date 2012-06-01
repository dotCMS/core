package com.dotmarketing.sitesearch.ajax;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.publishing.sitesearch.SiteSearchConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
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
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_SITESEARCH", user)) {
				String userName = map.get("u");
				String password = map.get("p");
				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
                    user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_SITESEARCH", user)){
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
	
	
	public void createSiteSearchIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		
		Map<String, String> map = getURIParams();
		int shards = 0;
		String alias="";
		try{
			shards = Integer.parseInt(map.get("shards"));
			alias = URLDecoder.decode((String) map.get("alias"), "UTF-8");
		}
		catch(Exception e){
			
		}

		String indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + ESContentletIndexAPI.timestampFormatter.format(new Date());
		APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName, alias, shards);

	}
	
	
	public void scheduleJob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		
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

		try {
			if(config.runNow()){
				APILocator.getSiteSearchAPI().executeTaskNow(config);
			}
			else{
				APILocator.getSiteSearchAPI().scheduleTask(config);
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
			String taskName = URLDecoder.decode((String) config.get("taskName"), "UTF-8");

			APILocator.getSiteSearchAPI().scheduleTask(config);
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
			writeError(response, e.getMessage());
			
		} 
	}
	
	
	
	
	public void deleteJob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {
		try {
			Map<String, String> map = getURIParams();
			String taskName = URLDecoder.decode(map.get("taskName"), "UTF-8");
			APILocator.getSiteSearchAPI().deleteTask(taskName);
		} catch (Exception e) {
			Logger.error(SiteSearchAjaxAction.class,e.getMessage(),e);
			writeError(response, e.getMessage());
			
		} 

	}
	
}
