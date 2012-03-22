package com.dotmarketing.viewtools;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

public class FavoritesWebAPI implements ViewTool {

	private static HttpServletRequest request;
	Context ctx;

	/**
	 * @param  obj  the ViewContext that is automatically passed on view tool initialization, either in the request or the application
	 * @return      
	 * @see         ViewTool, ViewContext
	 */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		request = context.getRequest();
		ctx = context.getVelocityContext();
	}

	public static boolean isUrlFavorite(String url_page, String url_params, String user_id) {
		boolean retVal = false;

		if (UtilMethods.isSet(url_params)) {
			url_page = url_page + "?" + url_params ;
		}

		DotConnect dotConnect = new DotConnect();
		StringBuffer sql = new StringBuffer("select * from user_favorites where page_url = ? and user_id = ?");

		dotConnect.setSQL(sql.toString());
		dotConnect.addParam(url_page);
		dotConnect.addParam(user_id);
		ArrayList results =null; 
		try {
			results = dotConnect.getResults();
		} catch (DotDataException e) {
		  Logger.error(FavoritesWebAPI.class,"isUrlFavorite method failed:"+ e, e);
		}
		
		if (results.size() > 0)
			retVal = true;

		return retVal;
	}

	public static boolean setUrlFavorite(String url_page, String url_params, String user_id) {
		boolean retVal = true;

		try {
			DotConnect dotConnect = new DotConnect();
	
			StringBuffer sql;
			if (!isUrlFavorite(url_page, url_params, user_id)) {
				sql = new StringBuffer("insert into user_favorites values (?, ?)");
			}
			else {
				sql = new StringBuffer("delete from user_favorites where user_id = ? and page_url = ?");
			}
	
			if (UtilMethods.isSet(url_params)) {
				url_page = url_page + "?" + url_params ;
			}

			dotConnect.setSQL(sql.toString());
			dotConnect.addParam(user_id);
			dotConnect.addParam(url_page);
			dotConnect.getResult();
		}
		catch (Exception e) {
			retVal = false;
		}

    	return retVal;
	}
	
	public static List getFavorites(String user_id) {
		List<Map> l = new ArrayList<Map>();

		try {
			Host host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);

			DotConnect dotConnect = new DotConnect();

			String sql = "select * from user_favorites where user_id = ?";

			dotConnect.setSQL(sql);
			dotConnect.addParam(user_id);
			ArrayList<HashMap> favorites = (ArrayList<HashMap>) dotConnect.getResults();

			for (HashMap<String, String> favorite : favorites) {
				String page_url = String.valueOf(favorite.get("page_url"));

				Map<String, String> hm = new HashMap<String, String>();
				hm.put("page_url", page_url);
				
				StringTokenizer strTok = new StringTokenizer(page_url, "?");
				String page_url_clean = strTok.nextToken();

				if (strTok.hasMoreElements() && (page_url.indexOf("pageTitle=") > -1)) {
					String url_params = strTok.nextToken();
					url_params = URLDecoder.decode(url_params);
					String pageTitle = url_params.substring((url_params.indexOf("pageTitle=") + 10), url_params.length());
					if (pageTitle.indexOf("&") > -1) {
						pageTitle = url_params.substring(0, pageTitle.indexOf("&"));
					}
					hm.put("page_title", pageTitle);

				}
				else {
					// Map with all identifier inodes for a given uri.
					Identifier idInode = APILocator.getIdentifierAPI().find(host, page_url_clean);
					HTMLPage livePage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(idInode,APILocator.getUserAPI().getSystemUser(),false);
	
					hm.put("page_title", livePage.getTitle());
				}

				l.add(hm);
			}
			request.getSession().setAttribute(WebKeys.USER_FAVORITES, l);
		}
		catch (Exception e) {

		}

		return l;
	}
}
