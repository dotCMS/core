package com.dotcms.analytics;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import io.vavr.control.Try;

import static com.dotcms.exception.ExceptionUtil.getErrorMessage;

/**
 * This utility class exposes common-use methods for the Analytics APIs.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class Util {

    /**
     * Based on the specified URL Map Context, determines whether a given incoming URL maps to a URL
     * Mapped content or not.
     *
     * @param pageMode   The {@link PageMode} used to display/render an HTML Page.
     * @param languageId The language ID used to display an HTML Page.
     * @param uri        The URI of the incoming request.
     * @param site       The {@link Host} where the HTML Page lives.
     *
     * @return If the URL maps to URL Mapped content, returns {@code true}.
     */
    public static boolean isUrlMap(final UrlMapContext urlMapContext) {
        return Try.of(() -> APILocator.getURLMapAPI().isUrlPattern(urlMapContext))
                .onFailure(e -> Logger.error(Util.class, String.format("Failed to check for URL Mapped content for page '%s': %s",
                        urlMapContext.getUri(), getErrorMessage(e)), e))
                .getOrElse(false);
    }

}
