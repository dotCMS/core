package com.dotcms.cdn.api;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;

public interface DotCDNAPI {

    static DotCDNAPI api(Host host) {
        return new DotCDNAPIImpl(host);
    }

    /**
     * Logic to get and parse the Stats from bunny to {@link DotCDNStats}
     * @param dateFromStr The start date of the statistics.
     * @param dateToStr The end date of the statistics
     * @return {@link DotCDNStats}
     */
    DotCDNStats getStats(final String dateFromStr, final String dateToStr);

    /**
     * Logic to invalidate the List of urls.
     * @param urls List of url to invalidate
     * @return true if all the urls were purged successfully, false if one or more failed.
     */
    boolean invalidate(final List<String> urls);

    /**
     * Invalidate all urls related to the contentlet
     * @param contentlet {@link Contentlet}
     * @return boolean
     */
    boolean invalidateContentlet(final Contentlet contentlet);

    /**
     * Invalidate all urls related to the contentlet, in addition can pass extra urls to purge
     * @param contentlet {@link Contentlet}
     * @param urlsToPurge {@link List}
     * @return boolean
     */
    boolean invalidateContentlet(final Contentlet contentlet, final List<String> urlsToPurge);

    /**
     * Invalidate all pages urls related to the contentlet, in addition can pass extra urls to purge
     * @param contentlet {@link Contentlet}
     * @param urlsToPurge {@link List}
     * @return boolean
     */
    boolean invalidateRelatedPages(final Contentlet contentlet, final List<String> urlsToPurge);

    /**
     * Logic to invalidate the entire cache.
     * @return true if the entire cache was invalidated successfully.
     */
    boolean invalidateAll();
}
