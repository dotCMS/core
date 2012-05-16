package com.dotmarketing.sitesearch.viewtool;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.publishing.sitesearch.SiteSearchResults;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;

public class SiteSearchWebAPI implements ViewTool {

	
	private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static SiteSearchAPI siteSearchAPI = APILocator.getSiteSearchAPI();
	private HttpServletRequest request;
	private HttpServletResponse response;

	public void init(Object initData) {
		ViewContext context = (ViewContext) initData;
		this.request = context.getRequest();
		this.response = context.getResponse();
	}
	
	/**
	 * Performs a search on the site search index using the current host in the request
	 * Sample usage from velocity:
	 * <pre>
     * {@code
	 * #set($searchresults = $sitesearch.search("dotcms",null,0,10,$request))
     * #set($hitsdetail = $searchresults.getDetails())
     * #set($summaries = $searchresults.getSummaries())
     * #foreach ($i in [0..$math.sub($searchresults.getEnd(),1)])
     *    $hitsdetail.get($i).getValue("title")
     *    $hitsdetail.get($i).getValue("url")
     *    $summaries.get($i).toHtml(true)
     * #end
     * }
     * </pre>
	 * @param query String to search for
	 * @param sort Property to sort the results
	 * @param start Start row
	 * @param rows  Number of rows to return (10 by default)
	 * @param request
	 * @return DotSearchResults
	 * @throws IOException
	 */

	public SiteSearchResults search(String query, String sort, int start, int rows)
			throws IOException {
		SiteSearchResults results= new SiteSearchResults();
		if(query ==null){
			results.setError("No query passed in");
			return results;
			
		}
		Host host = null;

		
		
		if(!StringUtils.isJson(query)){
			try {
				host = hostWebAPI.getCurrentHost(request);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				try {
					Logger.warn(this, "Error getting host from request, trying default host");
					host = hostWebAPI.findDefaultHost(userAPI.getSystemUser(), false);
				} catch (Exception e1) {
					Logger.error(this, e1.getMessage(), e1);
					results.setError("no host:" + e.getMessage());
					return results;
				}
			
			}

		}
		
				
		SiteSearchResults dsr = siteSearchAPI.search(query, sort, start, rows);


		return dsr;
	}
	
	


}
