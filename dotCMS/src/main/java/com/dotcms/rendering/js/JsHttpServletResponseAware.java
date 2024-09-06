package com.dotcms.rendering.js;

import javax.servlet.http.HttpServletResponse;

/**
 * Implement this class to get the response into the {@link JsViewTool}
 * @author jsanca
 */
public interface JsHttpServletResponseAware {

    void setResponse(final HttpServletResponse response);
}
