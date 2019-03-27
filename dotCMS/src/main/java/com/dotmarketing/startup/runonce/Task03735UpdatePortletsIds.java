package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * This startup task will update in the cms_layouts_portlets
 * the portlet ids withthe new values easier to read
 * 
 * @author oswaldogallango
 *
 */
public class Task03735UpdatePortletsIds implements StartupTask {

	private Map<String, String> portletIds = new HashMap<String,String>();

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		//update portlet names
		getPortletIds();
		DotConnect dc=new DotConnect();
		for(String key : portletIds.keySet()){
			dc.setSQL("update cms_layouts_portlets set portlet_id= ? where portlet_id= ?");
			dc.addParam(key);
			dc.addParam(portletIds.get(key));
			dc.loadResult();
		}	
		
		//remove deleted portlets
		dc.setSQL("delete from cms_layouts_portlets where portlet_id in ('EXT_3','EXT_ECO_ORDER','25','EXT_16','EXT_19','EXT_5','EXT_6','EXT_8','EXT_COMMUNICATIONS_MANAGER','EXT_CONTENTRATINGS','EXT_FACILITY','EXT_ORG','EXT_USERFILTER','EXT_USERMANAGER','EXT_USER_ACCOUNT_NOTES','REST_EXAMPLE_PORTLET','EXT_PRODUCT','EXT_USER_CLICKS','EXT_USER_COMMENTS','NetworkPortlet')");
		dc.loadResult();

		CacheLocator.getLayoutCache().clearCache();
	}

	/**
	 * Get the portlet Ids to update
	 */
	private void getPortletIds(){
		/*home */
		portletIds.put("dashboard","EXT_DASHBOARD");
		portletIds.put("workflow","EXT_21");
		
		/* site browser */
		portletIds.put("site-browser","EXT_BROWSER");
		portletIds.put("links","EXT_18");
		portletIds.put("templates","EXT_13");
		portletIds.put("containers","EXT_12");
		portletIds.put("time-machine","TIMEMACHINE");
		portletIds.put("publishing-queue","EXT_CONTENT_PUBLISHING_TOOL");
		
		/* content*/
		portletIds.put("content","EXT_11");
		portletIds.put("link-checker","EXT_BROKEN_LINKS");
		portletIds.put("calendar","EXT_CALENDAR");
		
		/* marketing*/
		portletIds.put("rules","RULES_ENGINE_PORTLET");
		portletIds.put("vanity-urls","EXT_VIRTUAL_LINKS");
		portletIds.put("forms","EXT_FORM_HANDLER");
		
		/* content types */
		portletIds.put("content-types","EXT_STRUCTURE");
		portletIds.put("categories","EXT_4");
		portletIds.put("tags","EXT_TAG_MANAGER");
		portletIds.put("workflow-schemes","WORKFLOW_SCHEMES");
		
		/* system */
		portletIds.put("configuration","9");
		portletIds.put("es-search","ES_SEARCH_PORTLET");
		portletIds.put("users","EXT_USER_ADMIN");
		portletIds.put("roles","EXT_ROLE_ADMIN");
		portletIds.put("sites","EXT_HOSTADMIN");
		portletIds.put("maintenance","EXT_CMS_MAINTENANCE");
		portletIds.put("languages","EXT_LANG");
		portletIds.put("query-tool","EXT_LUCENE_TOOL");
		portletIds.put("site-search","EXT_SITESEARCH");
		portletIds.put("dynamic-plugins","OSGI_MANAGER");
		
		/* extra */
		portletIds.put("html-pages","EXT_15");
		portletIds.put("personas","PERSONAS_PORTLET");
		portletIds.put("reports","EXT_REPORTMANAGER");
		portletIds.put("jobs","EXT_SCHEDULER");
		portletIds.put("director","EXT_17");
		portletIds.put("web-forms","EXT_WEBFORMS");
		
		portletIds.put("events","EXT_EVENTS");
		portletIds.put("events-approval","EXT_EVENTSAPPROVAL");
		portletIds.put("web-event-registrations","EXT_EVE_REG");
	}


}
