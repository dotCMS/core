package com.dotcms.rendering.js;

import com.dotcms.cache.DotJSONCache;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;

/**
 * Strategy to handle the response of the javascript execution
 * @author jsanca
 */
public interface JsResponseStrategy extends Serializable  {

    Response apply(final HttpServletRequest request, final HttpServletResponse response,
                   final User user, final DotJSONCache cache, final Map<String, Object> contextParams,
                   final Object result);
}
