package com.dotcms.ai.rest;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Resolves the target {@link Host} for a dotAI REST request from an optional {@code siteId}
 * query parameter, falling back to the host derived from the HTTP request when the parameter is
 * absent or unresolvable. Shared by every dotAI endpoint that reads or tests a per-site
 * {@code providerConfig} ({@link CompletionsResource}, {@link AiProviderResource}).
 */
public final class AiHostResolver {

    private AiHostResolver() {
    }

    /**
     * Resolves the caller, the target site and that site's dotAI configuration in one step.
     *
     * <p>This is the single entry point the {@code /api/inference/v1} family uses to obtain an
     * {@link AppConfig}; resolving a host and fetching a configuration separately is what allowed
     * two shipped dotAI endpoints to drift to opposite policies, and returning the three together
     * removes the opportunity.</p>
     *
     * <p>Resolution is deliberately <strong>standard</strong>. When {@code siteOverride} is blank
     * the site comes from the request exactly as it does everywhere else in dotCMS, default-site
     * fallback included, because the server-side callers this family exists for routinely present
     * a host name that is no site alias — internal DNS, container service names, {@code
     * localhost}. Refusing those would have broken the primary supported deployment. The risk the
     * fallback carries is that nobody can tell which site paid, so a fallback is logged here and
     * the resolved site is reported back on every response.</p>
     *
     * <p>An explicit override is the one path that enforces site READ: {@link #findHost} resolves
     * it as the caller, so a site they cannot see raises {@link DotSecurityException}.</p>
     *
     * @param request      the inbound request
     * @param siteOverride an explicit site id or host name, or null/blank to resolve from the request
     * @param user         the authenticated caller
     * @return the caller, the resolved site and its configuration
     * @throws DotSecurityException when an explicit override names a site the caller cannot read
     * @throws IllegalArgumentException when an explicit override names no known site
     */
    public static ResolvedAiContext resolve(final HttpServletRequest request,
                                            final String siteOverride,
                                            final User user) throws DotSecurityException {
        final Host host = StringUtils.isNotBlank(siteOverride)
                ? requireOverriddenHost(siteOverride, user)
                : resolveFromRequest(request);
        return new ResolvedAiContext(user, host, ConfigService.INSTANCE.config(host));
    }

    /**
     * Resolves an explicitly requested site, as the caller, so permissions apply.
     *
     * @param siteOverride the site id or host name the caller asked for
     * @param user         the authenticated caller
     * @return the site
     * @throws DotSecurityException when the caller cannot read it
     */
    private static Host requireOverriddenHost(final String siteOverride, final User user)
            throws DotSecurityException {
        try {
            final Host found = findHost(siteOverride, user);
            if (found == null) {
                throw new IllegalArgumentException("Site not found: " + sanitize(siteOverride));
            }
            return found;
        } catch (final DotSecurityException | IllegalArgumentException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Site not found: " + sanitize(siteOverride), e);
        }
    }

    /**
     * Resolves the site from the request, noting when the default site had to stand in.
     *
     * <p>The fallback itself is standard dotCMS behaviour and is kept. What is not kept is its
     * silence: an unmatched host name means some site's credentials are about to be spent on a
     * request that did not name it, and that should be visible in the log as well as in the
     * response.</p>
     *
     * @param request the inbound request
     * @return the resolved site
     */
    private static Host resolveFromRequest(final HttpServletRequest request) {
        final String serverName = request.getServerName();
        try {
            final Optional<Host> matched = APILocator.getHostAPI()
                    .resolveHostNameWithoutDefault(serverName, APILocator.systemUser(), false);
            if (matched.isPresent()) {
                return matched.get();
            }
            Logger.warn(AiHostResolver.class,
                    "Inference request for host '" + sanitize(serverName)
                            + "' matched no site or alias; the default site will serve it and its"
                            + " credentials will be spent. The serving site is reported on the response.");
        } catch (final Exception e) {
            Logger.warn(AiHostResolver.class,
                    "Could not resolve host '" + sanitize(serverName) + "': " + e.getMessage());
        }
        return defaultHost();
    }

    /**
     * The default site, resolved without consulting the request.
     *
     * <p>Deliberately not {@code getCurrentHostNoThrow}, which is the obvious call and the wrong
     * one: it reads the {@code host_id} and {@code Host} request parameters before it ever looks
     * at the server name (`HostWebAPIImpl.getCurrentHostFromRequest`), and FR-025 requires those
     * be ignored on this family. They were already ignored whenever the server name matched a
     * site, because that path returns before reaching here — so the effect was that a legacy
     * override was honoured in precisely the case where it could redirect which site's
     * credentials get spent, and ignored everywhere it was harmless.</p>
     *
     * @return the default site
     */
    private static Host defaultHost() {
        try {
            return APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        } catch (final Exception e) {
            throw new IllegalStateException("Could not resolve the default site: " + e.getMessage(), e);
        }
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

    public static String sanitize(final String value) {
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
