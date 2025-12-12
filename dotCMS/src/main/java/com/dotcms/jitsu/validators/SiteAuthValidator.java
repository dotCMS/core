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
        } catch (final DotDataException | DotSecurityException e) {
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
