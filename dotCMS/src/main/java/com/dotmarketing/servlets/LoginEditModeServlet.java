package com.dotmarketing.servlets;

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
			
			Identifier _edit_mode_id = new Identifier();
			_edit_mode_id.setURI("/"); 
			try{
				Clickstream clickstream = (Clickstream) request.getSession().getAttribute("clickstream");

				if (clickstream != null) {
					String pageId = clickstream.getLastPageId();
					_edit_mode_id = APILocator.getIdentifierAPI().find(pageId);

					if ("contentlet".equals(_edit_mode_id.getAssetType())) {
						com.dotmarketing.portlets.contentlet.model.Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(_edit_mode_id.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
						
						String pageURI = findAssetURI(cont);
						    
						_edit_mode_id.setURI(pageURI);
					}
				}else{
					Logger.info(LoginEditModeServlet.class,
							"The edit mode don't work when the ENABLE_CLICKSTREAM_TRACKING is off");
				}
			}
			catch(Exception e){
				Logger.error(this.getClass(), "unable to get last page: "  + e);
			}
			
			// this is used by the PortalRequestProcessort.java to set you up in edit mode
			request.getSession().setAttribute("LOGIN_TO_EDIT_MODE", _edit_mode_id);
		}
		else{
			request.getSession().removeAttribute("LOGIN_TO_EDIT_MODE");
			
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
		request.getRequestDispatcher("/c").forward(request,response);
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
