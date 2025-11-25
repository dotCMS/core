package com.dotcms.analytics;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import static com.dotcms.exception.ExceptionUtil.getErrorMessage;

/**
 * This utility class exposes common-use methods for the Analytics APIs.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class Util {

    private Util() {
        // Singleton
    }
    /**
     * Based on the specified URL Map Context, determines whether a given incoming URL maps to a URL
     * Mapped content or not.
     *
     * @param urlMapContext   UrlMapContext object containing the following information:
     * @return If the URL maps to URL Mapped content, returns {@code true}.
     */
    public static boolean isUrlMap(final UrlMapContext urlMapContext) {
        return Try.of(() -> APILocator.getURLMapAPI().isUrlPattern(urlMapContext))
                .onFailure(e -> Logger.error(Util.class, String.format("Failed to check for URL Mapped content for page '%s': %s",
                        urlMapContext.getUri(), getErrorMessage(e)), e))
                .getOrElse(false);
    }

}
