package com.dotcms.analytics.web;

import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Encapsulate the logic to interact with the analytics web API
 * For instance determine if injection is available or not
 * Retrieve the js code to inject, etc
 * @author jsanca
 */
public interface AnalyticsWebAPI extends EventSubscriber<SystemTableUpdatedKeyEvent> {

    /**
     * Returns true if the analytics auto injection is enabled and if there is any analytics app installed
     * @return boolean true if the analytics auto injection is enabled and if there is any analytics app installed
     */
     boolean isAutoJsInjectionEnabled(final HttpServletRequest request);

    /**
     * Returns true if the analytics auto injection flag is on
     * @return boolean true if the analytics auto injection flag is on
     */
    boolean isAutoJsInjectionFlagOn();

    /**
     * Returns true if there is any analytics app installed
     * @param request
     * @return
     */
     boolean anyAnalyticsConfig(final HttpServletRequest request);

    /**
     * Return the HTML/JS Code needed to support Analytics into the Browser
     * @param host
     * @param request
     * @return
     */
    Optional<String> getCode(final Host host, final HttpServletRequest request);

}
