package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is in charge of firing the collectors to populate the event payload. Also do the triggering the event to save the analytics data
 * @author jsanca
 */
public interface WebEventsCollectorService {

    void fireCollectors (final HttpServletRequest request, final HttpServletResponse response,
                        final RequestMatcher requestMatcher);


    /**
     * Add a collector
     * @param collectors
     */
    void addCollector(final Collector... collectors);

    /**
     * Remove a collector by id
     * @param collectorId
     */
    void removeCollector(final String collectorId);
}
