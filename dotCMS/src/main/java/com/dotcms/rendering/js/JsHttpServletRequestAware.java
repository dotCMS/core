package com.dotcms.rendering.js;

import javax.servlet.http.HttpServletRequest;

/**
 * Implement this class to get the request into the {@link JsViewTool}
 * @author jsanca
 */
public interface JsHttpServletRequestAware {

    void setRequest(final HttpServletRequest request);
}
