package com.dotcms.cdn.api;

import com.dotcms.cdn.CDNConstants;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;

public interface DotCDNAPI {

    static DotCDNAPI api(Host host) {
        return new DotCDNAPIImpl(host);
    }

    static boolean isConfigured(final Host host) {
        if (host == null || UtilMethods.isNotSet(host.getIdentifier())) {
            return false;
        }

        final Optional<AppSecrets> secrets = Try.of(() -> APILocator.getAppsAPI()
                .getSecrets(CDNConstants.DOT_CDN_APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());
        return secrets.isPresent();
    }

    /**
     * Logic to get and parse the Stats from bunny to {@link DotCDNStats}
     * @param dateFromStr The start date of the statistics.
     * @param dateToStr The end date of the statistics
     * @return {@link DotCDNStats}
     */
    default DotCDNStats getStats(final String dateFromStr, final String dateToStr) {
        return getStats(dateFromStr, dateToStr, false);
    }

    /**
     * Logic to get and parse the Stats from bunny to {@link DotCDNStats}
     * @param dateFromStr The start date of the statistics.
     * @param dateToStr The end date of the statistics
     * @param hourly If true, return hourly granularity instead of daily
     * @return {@link DotCDNStats}
     */
    DotCDNStats getStats(final String dateFromStr, final String dateToStr, final boolean hourly);

    /**
     * Reduces a URL to the path (and query) it names on this CDN's configured domain.
     *
     * <p>A path such as {@code /images/logo.png} is returned as it is. A full URL, with or without
     * a scheme, is reduced to its path when its host is the configured CDN domain, so
     * {@code https://cdn.example.com/images/*} becomes {@code /images/*}. The hosts are compared,
     * not the text, so {@code https://cdn.example.com.other.net/x} is not on {@code cdn.example.com}.
     *
     * @param url a path, or a URL that may be on the CDN domain
     * @return the path on the CDN domain, or empty when the URL is on any other host or is not a
     *         valid URL
     */
    Optional<String> toCdnPath(final String url);

    /**
     * Logic to invalidate the List of urls. Each URL is a path, or a URL on the CDN domain (see
     * {@link #toCdnPath(String)}), and is purged on the configured CDN domain. A URL on any other
     * host is skipped and counted as a failure.
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
