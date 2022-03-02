package com.dotcms.prerender;


import org.apache.http.HttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines the interface to customize and extends the default prerender functionality
 * @author jsanca
 */
public interface PreRenderEventHandler {

    String beforeRender(HttpServletRequest clientRequest);

    String afterRender(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse prerenderResponse, String responseHtml);

    void destroy();
}
