package com.dotcms.jitsu.validators;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.jitsu.validators.ValidationErrorCode.INVALID_SITE_AUTH;
import static com.liferay.util.StringPool.BLANK;

/**
 * This custom validator verifies that the Site auth being passed down when submitting a Content
 * Analytics Event correctly matches the one set in the {@code Content Analytics} App. If it
 * doesn't, then the Event cannot be trusted, and will be rejected.
 *
 * @author Jose Castro
 * @since Jul 4th, 2025
 */
public class SiteAuthValidator implements AnalyticsValidator {

    @Override
    public boolean test(final JSONObject jsonValidatorBody) {
        return jsonValidatorBody.has(CUSTOM_VALIDATOR_ATTRIBUTE) &&
                SiteAuthValidator.class.getSimpleName().equalsIgnoreCase(jsonValidatorBody.get(CUSTOM_VALIDATOR_ATTRIBUTE).toString());
    }

    @Override
    public void validate(final Object fieldValue) throws AnalyticsValidationException {
        boolean isKeyValid = false;
        final String siteAuth = Try.of(fieldValue::toString).getOrElse(BLANK);
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        Host currentSite = new Host();
        try {
            if (null != request) {
                currentSite = ContentAnalyticsUtil.getSiteFromRequest(request);
                final Optional<AppSecrets> secretsOpt =
                        APILocator.getAppsAPI().getSecrets(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, false, currentSite, APILocator.systemUser());
                if (secretsOpt.isPresent()) {
                    final Map<String, Secret> secretsMap = secretsOpt.get().getSecrets();
                    if (null != secretsMap.get("siteAuth")) {
                        final String siteAuthFromApp = secretsMap.get("siteAuth").getString();
                        if (UtilMethods.isSet(siteAuthFromApp) && siteAuthFromApp.equals(siteAuth)) {
                            isKeyValid = true;
                        }
                    }
                }
            } else {
                Logger.warn(this, "HTTP Request object could not be retrieved");
            }
            // Matched on the CAUSE CHAIN rather than the thrown type. Since issue #36724 an
            // unreadable secrets store raises instead of silently wiping itself, and that must
            // surface as an ordinary validation failure here rather than escaping raw onto the
            // analytics collection path. But SecretsStoreUnreadableException does not survive the
            // journey: several `catch (Exception e) { throw new DotRuntimeException(e); }` layers
            // sit between where it is raised and here -- SecretsKeyStoreHelper.loadValueFromStore
            // being the one on this path -- so catching the specific type alone never matches in
            // production.
            //
            // AnalyticsValidationException is deliberately NOT caught: it is this pipeline's own
            // signal, raised by ContentAnalyticsUtil when the site cannot be resolved from the
            // Origin/Referer headers, and already carries the message callers are meant to see.
            // Catching Exception here swallowed it and re-wrapped it, which regressed
            // AnalyticsValidatorUtilTest.
        } catch (final DotDataException | DotSecurityException | DotRuntimeException e) {

            if (e instanceof DotRuntimeException
                    && !ExceptionUtil.causedBy(e, SecretsStoreUnreadableException.class)) {
                // A database blip, a cache failure, a programming error -- stays loud rather than
                // being reported as an invalid site auth.
                throw (DotRuntimeException) e;
            }
            final String errorMsg = String.format("Site Auth for Site '%s' could not be verified: %s",
                    null != currentSite ? currentSite.getHostname() : BLANK, ExceptionUtil.getErrorMessage(e));
            Logger.warnAndDebug(SiteAuthValidator.class, errorMsg, e);
            throw new AnalyticsValidationException(errorMsg, INVALID_SITE_AUTH);
        }
        if (!isKeyValid) {
            throw new AnalyticsValidationException("Invalid Site Auth", INVALID_SITE_AUTH);
        }
    }
}
