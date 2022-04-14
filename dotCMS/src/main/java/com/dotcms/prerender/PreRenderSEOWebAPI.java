package com.dotcms.prerender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The prerender is a third party service that caches the final HTML page and stores the result in their system.
 * So when a bot (web spider) ask for page, it is identified (based on the agent header) and instead of building
 * the page in dotCMS, the final html rendered is delegate to the third-party system, render.io
 * This API allows to do the prerender if the current request is elegible to do it (based on the agent header)
 * @author jsanca
 */
public interface PreRenderSEOWebAPI {

    /**
     * Returns true if the current request is elegible for prerender (means it is a robot request).
     * The current response is overrided at the sametime, so the rest of the workflow should be skipped
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return boolean
     */
    boolean prerenderIfEligible(HttpServletRequest request, HttpServletResponse response);
}
