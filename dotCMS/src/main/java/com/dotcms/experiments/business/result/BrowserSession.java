package com.dotcms.experiments.business.result;

import com.dotcms.util.DotPreconditions;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represent a Session into a n Experiment define by the value of the {@link com.dotcms.experiments.model.Experiment#lookbackWindow}.
 *
 * For example if we have 4 pages let called then: A, B, C and D
 * Also the lookBackWindows expire after 30 minutes.
 *
 * If a User come into the site and navigate by several pages let say: A, B and C
 * and then after 60 minutes come again and navigate others pages: A, B and D
 * then we have two Browser Session each one with 3 pageview {@link Event}
 */
public class BrowserSession {

    private final List<Event> events;
    private final String lookBackWindow;

    public BrowserSession(final String lookBackWindow, final List<Event> events) {
        this.events = events;
        this.lookBackWindow = lookBackWindow;
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getLookBackWindow() {
        return lookBackWindow;
    }

    public Optional<String> getVariant() {
        if (!events.isEmpty()) {
            return events.get(0).getVariant();
        }

        return Optional.empty();
    }
}
