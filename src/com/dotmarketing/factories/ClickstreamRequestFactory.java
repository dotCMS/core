/*
 * Created on Jun 1, 2004
 *
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.dotmarketing.factories;

import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.HostWebAPIImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;

import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * @author rocco
 * @author davidtorresv
 */

public class ClickstreamRequestFactory {

	private static LanguageAPI langAPI =  APILocator.getLanguageAPI();
	
    public static ClickstreamRequest getClickstreamRequest(HttpServletRequest request, Date timestamp) {
        
        HttpSession session = request.getSession();
        long languageId = langAPI.getDefaultLanguage().getId();
        if (session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE) != null) {
            languageId = Long.parseLong(session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE).toString());
        }

        String uri = request.getRequestURI();
        if(request.getAttribute(WebKeys.CLICKSTREAM_URI_OVERRIDE) != null){
            uri = (String) request.getAttribute(WebKeys.CLICKSTREAM_URI_OVERRIDE);
        }
        
		HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
        Host host= null;
        
		try {
			host = hostWebAPI.getCurrentHost(request);
		} catch (PortalException e) {
    		Logger.error(ClickstreamRequestFactory.class, "Unable to retrieve current request host for URI " + uri);
		} catch (SystemException e) {
    		Logger.error(ClickstreamRequestFactory.class, "Unable to retrieve current request host for URI  " + uri);
		} catch (DotDataException e) {
    		Logger.error(ClickstreamRequestFactory.class, "Unable to retrieve current request host for URI  " + uri);
		} catch (DotSecurityException e) {
    		Logger.error(ClickstreamRequestFactory.class, "Unable to retrieve current request host for URI  " + uri);
		}
		
        String hostIdentifier = host.getIdentifier();
        
        ClickstreamRequest cr = new ClickstreamRequest();
        cr.setProtocol(request.getProtocol());
        cr.setServerName(request.getServerName());
        cr.setServerPort(request.getServerPort());
        cr.setQueryString(request.getQueryString());
        cr.setRemoteUser(request.getRemoteUser());
        cr.setRequestURI(uri);
        cr.setLanguageId(languageId);
        cr.setTimestamp(timestamp);
        cr.setHostId(hostIdentifier);
        return cr;
    }

    public static void save(ClickstreamRequest clickstreamRequest) {
        try {
			HibernateUtil.saveOrUpdate(clickstreamRequest);
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamRequestFactory.class, "save: failed", e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
    }

    @SuppressWarnings("unchecked")
	public static java.util.List<ClickstreamRequest> getClickstreamRequestsByRequestURI(String requestUri) {
        HibernateUtil dh = new HibernateUtil(ClickstreamRequest.class);
        try {
			dh.setSQLQuery("SELECT {clickstream_request.*}  FROM clickstream_request WHERE  request_uri = ? ORDER BY timestampper");
	        dh.setParam(requestUri);
	        return (List<ClickstreamRequest>) dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamRequestFactory.class, "getClickstreamRequestsByRequestURI: failed", e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
    }

    @SuppressWarnings("unchecked")
	public static java.util.List<ClickstreamRequest> getClickstreamRequestsByClickStream(Clickstream cs) {
        HibernateUtil dh = new HibernateUtil(ClickstreamRequest.class);
        try {
	        dh.setSQLQuery("SELECT    {clickstream_request.*}  FROM clickstream_request WHERE  clickstream_id = ? ORDER BY timestampper");
	        dh.setParam(cs.getClickstreamId());
	        return (List<ClickstreamRequest>)dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ClickstreamRequestFactory.class, "getClickstreamRequestsByClickStream: failed", e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

    }

}
