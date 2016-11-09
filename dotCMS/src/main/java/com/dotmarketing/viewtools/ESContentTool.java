package com.dotmarketing.viewtools;


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import org.elasticsearch.action.search.SearchResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.content.ContentMap;
import com.liferay.portal.model.User;

public class ESContentTool implements ViewTool {
	private UserWebAPI userAPI;

	private HttpServletRequest req;
	private User user = null;
	private boolean ADMIN_MODE;
	private boolean PREVIEW_MODE;
	private boolean EDIT_MODE;
	private boolean LIVE;
	private String tmDate;
	private Context context;
	ContentletAPI esapi = APILocator.getContentletAPI();
	private Host currentHost;
	
	@Override
	public void init(Object initData) {
		userAPI = WebAPILocator.getUserWebAPI();

		
		LIVE = true;
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();
		tmDate=null;
		ADMIN_MODE=false;
		PREVIEW_MODE=false;
		EDIT_MODE=false;
		HttpSession session = req.getSession(false);
		try {
			user = userAPI.getLoggedInFrontendUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}

		if(session!=null){
			tmDate = (String) session.getAttribute("tm_date");
			boolean tm=tmDate!=null;
			ADMIN_MODE = !tm && (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
			PREVIEW_MODE = !tm && ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
			EDIT_MODE = !tm && ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
			if(EDIT_MODE || PREVIEW_MODE){
				LIVE = false;
			}
		}
		try{
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		}catch(Exception e){
			Logger.error(this, "Error finding current host", e);
		}
		
	}
	
	
	
	public ESSearchResults search(String esQuery) throws DotSecurityException, DotDataException{
		
		ESSearchResults cons =  esapi.esSearch(esQuery, LIVE, user, true);
		List<ContentMap> maps = new ArrayList<ContentMap>();
		
		
		for(Object x : cons){
			Contentlet con = (Contentlet)x;

			maps.add(new ContentMap(con, user, !LIVE,currentHost,context));
		}
		
		return new ESSearchResults(cons.getResponse(), maps);
	}
	
	
	public SearchResponse raw(String esQuery) throws DotSecurityException, DotDataException{
		
		return esapi.esSearchRaw(esQuery, LIVE, user, true);
		
	}
	

}