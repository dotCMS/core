package com.dotmarketing.servlets;

import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class LoginEditModeServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5466263766587176193L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if(request.getRequestURI().indexOf("edit") > -1){


			try{
				final Clickstream clickstream = (Clickstream) request.getSession().getAttribute("clickstream");

				if (clickstream != null) {
					final Language lang = WebAPILocator.getLanguageWebAPI().getLanguage(request);
					String pageUrl = null;

					try {//Check if is a URL MAP Content Page
						final ClickstreamRequest click =  clickstream.getClickstreamRequests().get(clickstream.getClickstreamRequests().size()-1);
						if(click.getAssociatedIdentifier()!=null) {
							final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(click.getAssociatedIdentifier(), false, lang.getId(), APILocator.getUserAPI().getSystemUser(), false);
							final String pageDetailIdentifier = contentlet.getContentType().detailPage();
							if(UtilMethods.isSet(pageDetailIdentifier)){
								final Contentlet pageDetail = APILocator.getContentletAPI()
										.findContentletByIdentifier(pageDetailIdentifier, false,
												lang.getId(), APILocator.getUserAPI().getSystemUser(),
												false);
								final HTMLPageAsset pageDetailAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(pageDetail);
								pageUrl = APILocator.getIdentifierAPI().find(pageDetail.getIdentifier()).getParentPath() + pageDetailAsset.getPageUrl() + "?urlMap=" + contentlet.getIdentifier();
							}
						}
					}catch(Exception e){
						Logger.error(this.getClass(), "unable to urlmap content page: "  + e);
					}
					if(pageUrl==null) {
						final Contentlet con = APILocator.getContentletAPI()
								.findContentletByIdentifier(clickstream.getLastPageId(), false,
										lang.getId(), APILocator.getUserAPI().getSystemUser(),
										false);
						final HTMLPageAsset asset = APILocator.getHTMLPageAssetAPI().fromContentlet(con);
						pageUrl = APILocator.getIdentifierAPI().find(con.getIdentifier()).getParentPath() + asset.getPageUrl();
					}
					request.getSession().setAttribute(WebKeys.LOGIN_TO_EDIT_MODE, pageUrl);
				}
			}
			catch(Exception e){
				Logger.error(this.getClass(), "unable to get last page: "  + e);
			}

		}
		else{
			request.getSession().removeAttribute(WebKeys.LOGIN_TO_EDIT_MODE);
			
		}
		HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
		Host host;
		try {
			host = hostWebAPI.getCurrentHost(request);
			request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
		} catch (PortalException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (SystemException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(LoginEditModeServlet.class,e.getMessage(),e);
		}
		response.sendRedirect("/dotAdmin/?r=" + System.currentTimeMillis());
	}

    private String findAssetURI(Contentlet cont) throws DotDataException {
        if(UtilMethods.isSet(cont.getMap().get("URL_MAP_FOR_CONTENT"))) {
            return cont.getMap().get("URL_MAP_FOR_CONTENT").toString();
        } else {
            HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(cont);
            return page.getURI();
        }
    }
	
}
