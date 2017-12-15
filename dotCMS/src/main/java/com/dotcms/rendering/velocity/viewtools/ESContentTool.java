package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.content.elasticsearch.business.ESSearchResults;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.viewtools.content.ContentMap;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.action.search.SearchResponse;

import com.liferay.portal.model.User;

public class ESContentTool implements ViewTool {
	private UserWebAPI userAPI;

	private HttpServletRequest req;
	private User user = null;
	private Context context;
	ContentletAPI esapi = APILocator.getContentletAPI();
	private Host currentHost;
    private PageMode mode;
	@Override
	public void init(Object initData) {
		userAPI = WebAPILocator.getUserWebAPI();

		

		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();

		mode = PageMode.get(this.req);
		try {
			user = userAPI.getLoggedInFrontendUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}


		try{
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		}catch(Exception e){
			Logger.error(this, "Error finding current host", e);
		}
		
	}
	
	
	
	public ESSearchResults search(String esQuery) throws DotSecurityException, DotDataException{

		ESSearchResults cons =  esapi.esSearch(esQuery, mode.showLive, user, true);
		List<ContentMap> maps = new ArrayList<ContentMap>();
		
		
		for(Object x : cons){
			Contentlet con = (Contentlet)x;

			maps.add(new ContentMap(con, user, !mode.showLive,currentHost,context));
		}
		
		return new ESSearchResults(cons.getResponse(), maps);
	}
	
	
	public SearchResponse raw(String esQuery) throws DotSecurityException, DotDataException{
		
		return esapi.esSearchRaw(esQuery, mode.showLive, user, true);
		
	}
	

}