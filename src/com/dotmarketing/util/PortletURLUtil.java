/*
 * Created on Jul 14, 2004
 *
 */
package com.dotmarketing.util;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletConfigImpl;
import com.liferay.util.Validator;

/**
 * @author Maria
 * 
 */
public class PortletURLUtil {

	public static String getActionURL(ActionRequest req, String _windowState, Map _params) {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		return com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq, _windowState, _params);
	}

	public static String getActionURL(HttpServletRequest req, String _windowState, Map _params) {
		
		PortletConfigImpl portletConfig = (PortletConfigImpl) req.getAttribute(WebKeys.JAVAX_PORTLET_CONFIG);
		String portletName = null;
	
		if(portletConfig==null) {
			try {
				User user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
				List<Layout>layouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
				Layout layout = layouts.get(0);
				List<String> portletIds = layout.getPortletIds();
				portletName = portletIds.get(0);
				return getActionURL(req, layout.getId(), _windowState, _params, portletName);
			} catch (Exception e) {
				Logger.error(PortletURLUtil.class, e.toString(), e);
			}
		} else {
			portletName = portletConfig.getPortletId();
		}
		
		return getActionURL(req, _windowState, _params, portletName);
	
	}
	
	public static String getActionURL(HttpServletRequest req, String _windowState, Map _params, String portletName) {
		
		Layout layout = (Layout) req.getAttribute(WebKeys.LAYOUT);
		return getActionURL(req, layout.getId(), _windowState, _params, portletName);
	}
	
	public static String getActionURL(HttpServletRequest req, String layoutId, String _windowState, Map _params, String portletName) {
			
		com.liferay.portlet.PortletURLImpl portletURL = new com.liferay.portlet.PortletURLImpl(req, portletName, layoutId, true);

		try {
			if (Validator.isNotNull(_windowState)) {
				portletURL.setWindowState(new WindowState(_windowState));
			}

			portletURL.setSecure(req.isSecure());

			if (_params != null) {
				Logger.debug(Config.class, "Setting params=" + _params);
				portletURL.setParameters(_params);
			}


		} catch (Exception e) {
			Logger.warn(PortletURLUtil.class, e.toString(), e);
		}

		return stripProtocolAndHost(portletURL.toString());
	}

	public PortletURLUtil() {
		// empty constructor to call methods from velocity pages
	}

	public static String getRenderURL(HttpServletRequest req, String _windowState, Map _params) {
		PortletConfigImpl portletConfig = (PortletConfigImpl) req.getAttribute(WebKeys.JAVAX_PORTLET_CONFIG);
		String portletName = portletConfig.getPortletId();
		return getRenderURL(req, _windowState, _params, portletName);
	}
	
	public static String getRenderURL(HttpServletRequest req, String _windowState, Map _params, String portletName) {
		
		Layout layout = (Layout) req.getAttribute(WebKeys.LAYOUT);
		 return getRenderURL(req, layout.getId(),_windowState, _params, portletName);
	}
	
	public static String getRenderURL(HttpServletRequest req, String layoutId, String _windowState, Map _params, String portletName) {
		
		com.liferay.portlet.PortletURLImpl portletURL = 
			new com.liferay.portlet.PortletURLImpl(req, portletName, layoutId, false);

		try {
			if (Validator.isNotNull(_windowState)) {
				portletURL.setWindowState(new WindowState(_windowState));
			}

			portletURL.setSecure(req.isSecure());

			if (_params != null) {
				Logger.debug(PortletURLUtil.class, "Setting params=" + _params);
				portletURL.setParameters(_params);
			}
		} catch (Exception e) {
			Logger.error(PortletURLUtil.class, e.toString(), e);
		}
		return stripProtocolAndHost(portletURL.toString());
	}
	
	/**
	 * this method returns a relative url from an absolute url
	 * @param url the full url + uri to be stripped
	 * @return the new url without protocol or host information
	 */
	
	
	private static String stripProtocolAndHost(String url){
	
		if(url.indexOf("://")<0){
			return url;
		}
		StringTokenizer st = new StringTokenizer(url, "/");
		StringBuffer sb = new StringBuffer();
		int i = 0;

		while (st.hasMoreTokens()) {
			String _y = st.nextToken();
			if (i > 1) {
				sb.append("/");
				sb.append(_y);
			}
			i++;
		}
		//return portletURL.toString();
		return sb.toString();
		
		
	}
	
	
	
}
