package com.dotmarketing.business;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Arrays;
import java.util.Set;
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


    static final AtomicLong expired = new AtomicLong();
    static final long REFRESH_INTERVAL = 1000 * 60 * 5;     // 5 minutes
    static volatile String[] _ignoreParams = new String[0];
    static volatile String[] _respectParams = new String[0];
    final private String[] params;

    public PageCacheParameters(final String... values) {
        this.params = filterNulls(values);
    }

    static String[] filterNulls(String[] values) {
        return Arrays.stream(values).filter(UtilMethods::isSet).toArray(String[]::new);
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
     *
     * @param queryString the query string to be filtered; may include key-value pairs separated by `&` or `?`
     * @return a filtered and alphabetically sorted query string containing the selected parameters,
     *         or {@code null} if the provided query string is empty
     */
    public static String filterQueryString(String queryString) {
        if (UtilMethods.isEmpty(queryString)) {
            return null;
        }
        // sorts the query string parameters alphabetically
        Set<String> querySplit = new TreeSet<>(Arrays.asList(filterNulls(queryString.toLowerCase().split("[?&]", -1))));
        StringBuilder queryStringBuilder = new StringBuilder();
        if (getRespectParams().length > 0) {
            for (String queryParam : querySplit) {
                if (matches(queryParam, getRespectParams())) {
                    queryStringBuilder.append("&").append(queryParam);
                }
            }
            return queryStringBuilder.toString();
        }


        for (String queryParam : querySplit) {
            if (!matches(queryParam, getIgnoreParams())) {
                queryStringBuilder.append("&").append(queryParam);
            }
        }
        return queryStringBuilder.toString();

    }


    /**
     * Generates the page subkey that will be used in the page cache. This key will represent a specific version of the
     * page.
     *
     * @return The subkey which is specific for a page.
     */
    public String getKey() {

        final String key = String.join(",", this.params);
        Logger.info(this.getClass(), () -> "page_cache_key:" + key);
        if( UtilMethods.isEmpty(key)){
            throw new DotRuntimeException("Page cache key is empty");
        }
        return key;


    }
}
