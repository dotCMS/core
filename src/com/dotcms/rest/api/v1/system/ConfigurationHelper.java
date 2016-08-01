package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.HttpRequestDataUtil.getHostname;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_ENDPOINTS;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_BASEURL;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_PROTOCOL;
import static com.dotmarketing.util.WebKeys.WEBSOCKET_SYSTEMEVENTS_ENDPOINT;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.util.Config;

/**
 * A utility class that provides the required dotCMS configuration properties to
 * the {@link ConfigurationResource} end-point. The idea behind this approach is
 * to provide other system modules - such as the UI - with access to specific 
 * system properties as they are needed.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 26, 2016
 *
 */
@SuppressWarnings("serial")
public class ConfigurationHelper implements Serializable {

	public static ConfigurationHelper INSTANCE = new ConfigurationHelper();

	/**
	 * Private class constructor for Singleton instantiation.
	 */
	private ConfigurationHelper() {

	}

	/**
	 * Reads the required configuration properties from the dotCMS configuration
	 * files and also from the {@link HttpServletRequest} object.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return A {@link Map} with all the required system properties.
	 */
	public Map<String, Object> getConfigProperties(final HttpServletRequest request) {
		return map(
				DOTCMS_WEBSOCKET_PROTOCOL,
				Config.getStringProperty(DOTCMS_WEBSOCKET_PROTOCOL, "ws"),
				DOTCMS_WEBSOCKET_BASEURL,
				Config.getAsString(DOTCMS_WEBSOCKET_BASEURL, () -> getHostname(request)),
				DOTCMS_WEBSOCKET_ENDPOINTS,
				map(WEBSOCKET_SYSTEMEVENTS_ENDPOINT,
						Config.getStringProperty(WEBSOCKET_SYSTEMEVENTS_ENDPOINT, "/api/v1/system/events")));
	}

}
