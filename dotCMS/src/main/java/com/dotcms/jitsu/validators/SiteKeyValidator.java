package com.dotcms.jitsu.validators;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.URLUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.jitsu.validators.ValidationErrorCode.INVALID_SITE_KEY;
import static com.dotcms.jitsu.validators.ValidationErrorCode.REQUIRED_FIELD_MISSING;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static com.liferay.util.StringPool.BLANK;

/**
 * This custom validator verifies that the Site Key being passed down when submitting a Content
 * Analytics Event correctly matches the one set in the {@code Content Analytics} App. If it
 * doesn't, then the Event cannot be trusted, and will be rejected.
 *
 * @author Jose Castro
 * @since Jul 4th, 2025
 */
public class SiteKeyValidator implements AnalyticsValidator {

    @Override
    public boolean test(final JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(CUSTOM_VALIDATOR_ATTRIBUTE) &&
                SiteKeyValidator.class.getSimpleName().equalsIgnoreCase(jsonValidatorBody.get(CUSTOM_VALIDATOR_ATTRIBUTE).toString());
    }

    @Override
    public void validate(final Object fieldValue) throws AnalyticsValidationException {
        boolean isKeyValid = true;
        final String siteKey = fieldValue.toString();
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        Host currentSite = new Host();
        try {
            currentSite = this.getSiteFromRequest(request);
            final Optional<AppSecrets> secretsOpt = APILocator.getAppsAPI().getSecrets(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, false, currentSite, APILocator.systemUser());
            if (secretsOpt.isPresent()) {
                final Map<String, Secret> secretsMap = secretsOpt.get().getSecrets();
                if (null != secretsMap.get("siteKey")) {
                    final String siteKeyFromApp = secretsMap.get("siteKey").getString();
                    if (!siteKeyFromApp.equals(siteKey)) {
                        isKeyValid = false;
                    }
                }
            }
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Site Key for Site '%s' could not be verified: %s",
                    null != currentSite ? currentSite.getHostname() : BLANK, ExceptionUtil.getErrorMessage(e));
            Logger.warnAndDebug(ContentAnalyticsUtil.class, errorMsg, e);
            throw new AnalyticsValidationException(errorMsg, INVALID_SITE_KEY);
        }
        if (!isKeyValid) {
            throw new AnalyticsValidationException("Invalid Site Key", INVALID_SITE_KEY);
        }
    }

    /**
     * Determines the Site that the Event is being sent to. This method looks for the current Site
     * in the HTTP Request object.
     *
     * @param request The current instance of the {@link HttpServletRequest}.
     *
     * @return The Site that the Event is being sent to.
     */
    private Host getSiteFromRequest(final HttpServletRequest request) throws AnalyticsValidationException {
        final Optional<String> siteFromRequestOpt = this.getSiteAliasFromRequest(request);
        if (siteFromRequestOpt.isEmpty()) {
            throw new AnalyticsValidationException("Site could not be retrieved from Origin or Referer HTTP Headers", REQUIRED_FIELD_MISSING);
        }
        final HostAPI hostAPI = APILocator.getHostAPI();
        try {
            Host currentSite = hostAPI.findByName(siteFromRequestOpt.get(), APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
            if (null == currentSite) {
                currentSite = hostAPI.findByAlias(siteFromRequestOpt.get(), APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES);
                if (null == currentSite) {
                    throw new AnalyticsValidationException(String.format("Site with name/alias '%s' was not found", siteFromRequestOpt.get()), REQUIRED_FIELD_MISSING);
                }
            }
            return currentSite;
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Site with name/alias '%s' could not be found: %s",
                    siteFromRequestOpt.get(), ExceptionUtil.getErrorMessage(e));
            Logger.error(ContentAnalyticsUtil.class, errorMsg, e);
            throw new AnalyticsValidationException(errorMsg, INVALID_SITE_KEY);
        }
    }

    /**
     * Extracts the site alias (domain) from the HTTP request.
     * First tries to get it from the Origin header, then falls back to the Referer header.
     * If a URL is found, it extracts just the host/domain part.
     *
     * @param request The HTTP request to extract the site alias from
     * @return The extracted site alias (domain) or null if not found
     */
    private Optional<String> getSiteAliasFromRequest(final HttpServletRequest request) {
        String siteUrl = request.getHeader(HttpHeaders.ORIGIN);
        if (UtilMethods.isNotSet(siteUrl)) {
            siteUrl = request.getHeader(HttpHeaders.REFERER);
        }
        // If we have a URL, extract just the host/domain part
        if (UtilMethods.isSet(siteUrl)) {
            try {
                final URLUtils.ParsedURL parsedUrl = URLUtils.parseURL(siteUrl);
                if (parsedUrl != null && UtilMethods.isSet(parsedUrl.getHost())) {
                    return Optional.of(parsedUrl.getHost());
                }
            } catch (final IllegalArgumentException e) {
                Logger.warn(ContentAnalyticsUtil.class, String.format("Site Alias could not be retrieved from HTTP Request: " +
                        "%s", ExceptionUtil.getErrorMessage(e)));
            }
        }
        return Optional.empty();
    }

}
