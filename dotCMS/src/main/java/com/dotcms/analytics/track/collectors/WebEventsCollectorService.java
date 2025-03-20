package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Map;

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

    /**
     * Allows to fire the collectors and emit the event from a base payload map already built by
     * the user
     *
     * @param request        The current instance of the {@link HttpServletRequest}.
     * @param response       The current instance of the {@link HttpServletResponse}.
     * @param requestMatcher The {@link RequestMatcher} that matched the dotCMS object being
     *                       processed, such as: HTML Page, File Asset, URL Mapped Content,
     *                       Vanity URL, etc.
     * @param basePayloadMap A Map containing all payload properties that were given user
     * @param baseContextMap A Map containing all context the properties that were given user
     */
    void fireCollectorsAndEmitEvent(final HttpServletRequest request, final HttpServletResponse response,
                                    final RequestMatcher requestMatcher, final Map<String, Serializable> basePayloadMap,
                                    Map<String, Object> baseContextMap);

}
