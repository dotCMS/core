package com.dotmarketing.cms.myaccount.action;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.viewtools.FavoritesWebAPI;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

public class MyFavoritesAction extends DispatchAction {

	private HostAPI hostAPI = APILocator.getHostAPI();
	
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String url_page = request.getParameter("url_page");
		String url_params = request.getParameter("url_params");
		if (!UtilMethods.isSet(url_params)) {
			url_params = "";
		}
		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			String path = URLEncoder.encode("/dotCMS/addFavorites?url_page="+url_page+"&url_params="+url_params);
			return new ActionForward(SecurityUtils.stripReferer("/dotCMS/login?referrer="+path));
		}

		//Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String user_id = user.getUserId();

		if (UtilMethods.isSet(url_page)) {
			if (FavoritesWebAPI.setUrlFavorite(url_page, url_params, user_id)) {
				List<Map> favorites = (List<Map>) request.getSession().getAttribute(WebKeys.USER_FAVORITES);
				if (!UtilMethods.isSet(favorites)) {
					favorites = new ArrayList<Map>();
				}
				Host host = hostAPI.findDefaultHost(user, false);
				Map<String, String> hm = new HashMap<String, String>();

				if (UtilMethods.isSet(url_params) && (url_params.indexOf("pageTitle=") > -1)) {
					String url_params_decoded = URLDecoder.decode(url_params);
					String pageTitle = url_params_decoded.substring((url_params_decoded.indexOf("pageTitle=") + 10), url_params_decoded.length());
					if (pageTitle.indexOf("&") > -1) {
						pageTitle = url_params.substring(0, pageTitle.indexOf("&"));
					}
					hm.put("page_title", pageTitle);

				}
				else {
					// Map with all identifier inodes for a given uri.
					Identifier idInode = APILocator.getIdentifierAPI().find( host,url_page);
					HTMLPage livePage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(idInode, APILocator.getUserAPI().getSystemUser(), false);
					hm.put("page_title", livePage.getTitle());
				}

				hm.put("page_url", UtilMethods.isSet(url_params)?url_page + "?" + url_params:url_page);

				if (FavoritesWebAPI.isUrlFavorite(url_page, url_params, user_id)) {
					favorites.add(hm);
					SessionMessages.add(request.getSession(), "message", "message.favorites.added");
				}
				else {
					favorites.remove(hm);
					SessionMessages.add(request.getSession(), "message", "message.favorites.removed");
				}
				request.getSession().setAttribute(WebKeys.USER_FAVORITES, favorites);
			}
			else {
				SessionMessages.add(request.getSession(), "error", "message.favorites.error");
			}
		}

		ActionForward af = new ActionForward(UtilMethods.isSet(url_params)?url_page + "?" + url_params:url_page);
        af.setRedirect(true);
        return af;
	}

}
