package com.dotmarketing.business;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class used to keep the parameters used to identify a page cache.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class PageCacheParameters {

    /**
     * ThreadLocal TreeSet for sorting query parameters without allocating a new Set per request.
     * TreeSet provides natural ordering which ensures consistent cache key generation.
     */
    private static final ThreadLocal<TreeSet<String>> QUERY_SET_LOCAL =
            ThreadLocal.withInitial(TreeSet::new);

    /**
     * ThreadLocal StringBuilder for building filtered query strings without allocation per request.
     */
    private static final ThreadLocal<StringBuilder> QUERY_BUILDER_LOCAL =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    static final AtomicLong expired = new AtomicLong();
    static final long REFRESH_INTERVAL = 1000 * 60 * 5;     // 5 minutes
    static volatile String[] _ignoreParams = new String[0];
    static volatile String[] _respectParams = new String[0];
    private String[] params = null;

    public PageCacheParameters(final String... values) {
        this.params = (values != null) ? filterNulls(values) : new String[0];
    }

    static String[] filterNulls(String[] values) {
        return values == null ? new String[0] : Arrays.stream(values).filter(UtilMethods::isSet).toArray(String[]::new);
    }

    /**
     * Gets the query parameters that will be ignored when generating the page cache key.
     * @return
     */
    static final String[] getIgnoreParams() {
        if (expired.get() < System.currentTimeMillis()) {
            refreshValues();
        }
        return _ignoreParams;
    }

    /**
     * Gets the query parameters that will be used to generate the page cache key.
     * @return
     */
    static final String[] getRespectParams() {
        if (expired.get() < System.currentTimeMillis()) {
            refreshValues();
        }
        return _respectParams;
    }

    // we can ignore this because we pass the language in separatly
    final static String ALWAYS_IGNORE = "com.dotmarketing.htmlpage.language";


    /**
     * Refreshes the values of the ignore and respect parameters.
     */
    static synchronized void refreshValues() {
        if (expired.get() < System.currentTimeMillis()) {
            String ignoreParams = Config.getStringProperty("PAGE_CACHE_IGNORE_PARAMETERS", "").toLowerCase();
            String respectParams = Config.getStringProperty("PAGE_CACHE_RESPECT_PARAMETERS", "").toLowerCase();
            _ignoreParams = filterNulls(
                    Arrays.stream((ignoreParams + "," + ALWAYS_IGNORE).split("[,;]", -1)).map(String::trim)
                            .toArray(String[]::new));
            _respectParams = filterNulls(
                    Arrays.stream((respectParams).split("[,;]", -1)).map(String::trim).toArray(String[]::new));
            expired.set(System.currentTimeMillis() + REFRESH_INTERVAL);
        }
    }



    static boolean matches(String test, String[] params) {
        if (test == null || params == null) {
            return false;
        }
        for (String param : params) {
            if (param != null && test.startsWith(param + "=")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filters and sorts the provided query string based on specific conditions.
     * The method processes the query string by splitting it into parameters,
     * normalizing (lowercasing) the values, sorting them alphabetically,
     * and determining which parameters to include or exclude
     * based on the "respect" or "ignore" parameters defined in the system.
     * <p>
     * Uses ThreadLocal buffers to minimize object allocation per request.
     *
     * @param queryString the query string to be filtered; may include key-value pairs separated by `&` or `?`
     * @return a filtered and alphabetically sorted query string containing the selected parameters,
     *         or {@code null} if the provided query string is empty
     */
    public static String filterQueryString(final String queryString) {
        if (UtilMethods.isEmpty(queryString)) {
            return null;
        }

        // Get and reset ThreadLocal buffers
        final TreeSet<String> querySplit = QUERY_SET_LOCAL.get();
        final StringBuilder queryStringBuilder = QUERY_BUILDER_LOCAL.get();
        querySplit.clear();
        queryStringBuilder.setLength(0);

        // Split, filter nulls, and add to TreeSet for sorting
        final String[] parts = queryString.toLowerCase().split("[?&]", -1);
        for (final String part : parts) {
            if (UtilMethods.isSet(part)) {
                querySplit.add(part);
            }
        }

        // Build filtered query string
        if (getRespectParams().length > 0) {
            for (final String queryParam : querySplit) {
                if (matches(queryParam, getRespectParams())) {
                    queryStringBuilder.append('&').append(queryParam);
                }
            }
            return queryStringBuilder.length() > 0 ? queryStringBuilder.toString() : null;
        }

        for (final String queryParam : querySplit) {
            if (!matches(queryParam, getIgnoreParams())) {
                queryStringBuilder.append('&').append(queryParam);
            }
        }
        return queryStringBuilder.length() > 0 ? queryStringBuilder.toString() : null;
    }


    private final Lazy<String> lazyKey = Lazy.of(() -> {
        final String key = String.join(",", this.params);
        Logger.debug(this.getClass(), () -> "page_cache_key:" + key);
        if( UtilMethods.isEmpty(key)){
            throw new DotRuntimeException("Page cache key is empty");
        }
        return key;

    });

    /**
     * Generates the page subkey that will be used in the page cache. This key will represent a specific version of the
     * page.
     *
     * @return The subkey which is specific for a page.
     */
    public String getKey() {

        return lazyKey.get();


    }
}
