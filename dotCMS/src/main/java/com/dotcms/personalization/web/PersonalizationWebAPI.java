package com.dotcms.personalization.web;

import javax.servlet.http.HttpServletRequest;

public interface PersonalizationWebAPI {

    /**
     * Gets the personalization for a container
     * This method will tries to figure out the personalization based on the current thread local context, if not will return just {@link MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * @return String
     */
    String getContainerPersonalization ();

    /**
     * Gets the personalization for a container
     * If request is null will return  {@link com.dotmarketing.beans.MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * @param request {@link HttpServletRequest}
     * @return String
     */
    String getContainerPersonalization (final HttpServletRequest request);

}
