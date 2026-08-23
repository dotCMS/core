package com.dotcms.ai.rest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves the target {@link Host} for a dotAI REST request from an optional {@code siteId}
 * query parameter, falling back to the host derived from the HTTP request when the parameter is
 * absent or unresolvable. Shared by every dotAI endpoint that reads or tests a per-site
 * {@code providerConfig} ({@link CompletionsResource}, {@link AiProviderResource}).
 */
final class AiHostResolver {

    private AiHostResolver() {
    }

    /**
     * Resolves a host from {@code siteId} and falls back to the HTTP host on failure.
     * Throws {@link DotSecurityException} when the user lacks permission for the requested site.
     * Falls back to the HTTP-derived host when {@code siteId} is blank or not found.
     */
    static Host resolveHost(final String siteId,
                           final HttpServletRequest request,
                           final User user) throws DotSecurityException {
        if (StringUtils.isNotBlank(siteId)) {
            try {
                final Host found = findHost(siteId, user);
                if (found != null) {
                    return found;
                }
            } catch (final DotSecurityException e) {
                throw e;
            } catch (final Exception e) {
                Logger.warn(AiHostResolver.class,
                        "Could not resolve siteId '" + sanitize(siteId) + "', falling back to current host: " + e.getMessage());
            }
        }
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
    }

    /**
     * Resolves a host from {@code siteId} strictly — no fallback.
     * Falls back to the HTTP-derived host when siteId is blank.
     * Returns {@code null} when the site is not found.
     * Throws {@link DotSecurityException} when the user lacks permission.
     * Use for write operations where silently targeting the wrong site is unacceptable.
     */
    static Host resolveHostStrict(final String siteId,
                                  final HttpServletRequest request,
                                  final User user) throws DotSecurityException {
        if (StringUtils.isBlank(siteId)) {
            return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        }
        try {
            return findHost(siteId, user);
        } catch (final DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            Logger.warn(AiHostResolver.class, "Could not resolve siteId '" + sanitize(siteId) + "': " + e.getMessage());
            return null;
        }
    }

    static String sanitize(final String value) {
        return value == null ? "null" : value.replaceAll("[\r\n\t]", "_");
    }

    private static Host findHost(final String siteId, final User user) throws Exception {
        if ("SYSTEM_HOST".equalsIgnoreCase(siteId)) {
            return APILocator.systemHost();
        }
        final Host found = APILocator.getHostAPI().find(siteId, user, false);
        return (found != null && StringUtils.isNotBlank(found.getIdentifier()) && !found.isArchived()) ? found : null;
    }

}
