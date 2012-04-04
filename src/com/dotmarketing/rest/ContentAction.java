package com.dotmarketing.rest;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
/**
 * Example uses
 * The live version of a piece of content, default language
 * GET /rest/content/id/1e1b073f-5a12-425e-a00b-b986fcecdb5d/state/live
 * 
 * The working version of a piece of content,languageId=1
 * GET /rest/content/id/1e1b073f-5a12-425e-a00b-b986fcecdb5d/state/working/language/1
 * 
 * A piece of content by Inode, passing in the user and password (watch out for clear text!)
 * GET /rest/content/inode/1e1b073f-5a12-425e-a00b-b986fcecdb5d/u/admin@dotcms.com/p/admin
 * 
 * A piece of content by inode, with the velocity fields rendered (remote widgets)
 * GET /rest/content/inode/1e1b073f-5a12-425e-a00b-b986fcecdb5d/render/true  
 * This would pass in the request and response and then return the rendered resultant velocity
 * 
 * Passed in lucene query
 * GET /rest/content/query/UrlEncoded($lucene_query)
 * 
 * Editing a piece of content
 * PUT /rest/content {json} for edit or update
 * 
 * Editing a piece of content
 * POST /rest/content {json} for edit or update
 * 
 * archiving a piece of content
 * DELETE /rest/content/id/1e1b073f-5a12-425e-a00b-b986fcecdb5d/state/archived
 * 
 * deleting a piece of content
 * DELETE /rest/content/id/1e1b073f-5a12-425e-a00b-b986fcecdb5d/state/delete
 * @author will
 *
 */
public class ContentAction extends RestAction {
	ContentletAPI  capi = APILocator.getContentletAPI();
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> params  = getURIParams();
		
		
		
		
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ParamMap<String, String> params  = getURIParams();
		String id = params.get("id");
		String inode = params.get("inode");
		String query = params.get("query");
		boolean live = params.getBoolean("live",false);
		long languageId = params.getLong("language", APILocator.getLanguageAPI().getDefaultLanguage().getId());
		if(isSet(id)){
			try {
				capi.findContentletByIdentifier(id, live, languageId, getUser(), true);
			} catch (DotContentletStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DotDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DotSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		
		
	}
	
	


}
