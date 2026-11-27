package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maximum number of results a content search request may ask for.
 *
 * <p>Request handlers that take a limit, a page size or a {@code per_page} value from the caller
 * pass it through {@link #cap(int)} before running a content search. Values up to the maximum are
 * kept, larger values are lowered to it, and zero or negative values are returned unchanged so each
 * caller keeps the meaning it gives them (its default page size, or "no limit given"). A maximum
 * lowered through the property therefore applies to the values callers send, not to those
 * defaults; an index search treats zero or a negative limit as a page of
 * {@value #DEFAULT_MAX_LIMIT}.
 *
 * <p>The maximum is read from {@value #MAX_LIMIT_PROPERTY} (environment variable
 * {@code DOT_CONTENT_SEARCH_MAX_LIMIT}) on every call. It defaults to {@value #DEFAULT_MAX_LIMIT},
 * the largest page a single index search returns
 * ({@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#MAX_LIMIT}); the property
 * can lower it but not raise it. A value that is not a positive number is ignored, the default is
 * used, and a warning is logged once for each such value.
 *
 * <p>Internal callers that read every match on purpose (exports, scheduled jobs) do not go through
 * this class and are not affected.
 */
public final class ContentSearchLimit {

    /** Property that lowers the maximum number of results a content search request may ask for. */
    public static final String MAX_LIMIT_PROPERTY = "content.search.max.limit";

    /** Default maximum, and the highest value the property may set. */
    public static final int DEFAULT_MAX_LIMIT = 10000;

    private static final AtomicReference<String> lastReportedValue = new AtomicReference<>();

    private ContentSearchLimit() {
    }

    /**
     * Returns the limit to use for a content search requested with the given limit: the requested
     * value when it is not above the maximum, the maximum otherwise. Zero and negative values are
     * returned unchanged.
     *
     * @param requested the limit, page size or {@code per_page} value sent by the caller
     * @return the limit to search with
     */
    public static int cap(final int requested) {
        final int maximum = maximum();
        return requested > maximum ? maximum : requested;
    }

    /**
     * Reads the configured maximum, falling back to {@link #DEFAULT_MAX_LIMIT} when the property is
     * unset or not a positive number, and never going above it.
     *
     * @return the maximum number of results a request may ask for
     */
    private static int maximum() {
        // Read as text: Config.getIntProperty throws on a value that is not a number
        final String value = Config.getStringProperty(MAX_LIMIT_PROPERTY, null);
        if (value == null) {
            return DEFAULT_MAX_LIMIT;
        }
        final int configured = Try.of(() -> Integer.parseInt(value)).getOrElse(0);
        if (configured < 1) {
            if (!Objects.equals(lastReportedValue.getAndSet(value), value)) {
                Logger.warn(ContentSearchLimit.class, MAX_LIMIT_PROPERTY
                        + " must be a positive number; using the default of " + DEFAULT_MAX_LIMIT);
            }
            return DEFAULT_MAX_LIMIT;
        }
        return Math.min(configured, DEFAULT_MAX_LIMIT);
    }
}
