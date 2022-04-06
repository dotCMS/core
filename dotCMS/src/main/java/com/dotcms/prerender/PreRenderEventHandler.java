package com.dotcms.prerender;


import org.apache.http.HttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines the interface to customize and extends the default prerender functionality
 * @author jsanca
 */
public interface PreRenderEventHandler {

    /**
     * Do something before the render
     * @param clientRequest {@link HttpServletRequest}
     * @return
     */
    String beforeRender(HttpServletRequest clientRequest);

    /**
     * Do something after the render
     * @param clientRequest
     * @param clientResponse
     * @param prerenderResponse
     * @param responseHtml
     * @return
     */
    String afterRender(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse prerenderResponse, String responseHtml);

    void destroy();
}
