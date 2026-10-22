package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.external.MetaData;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.handler.AssertionResolverHandler;
import com.dotcms.saml.service.internal.AssertionReplayStore;
import com.dotcms.saml.service.internal.EndpointService;
import com.dotcms.saml.service.internal.MetaDataService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates a decoded SAML Response and its assertion against the SAML 2.0 Web Browser SSO profile
 * (Profiles §4.1.4.2-4.1.4.3, Core §2.5.1, Bindings §3.5.5.2). It runs after the signatures have been
 * verified and checks everything a valid signature does not prove on its own:
 * <ul>
 *     <li>the Response Destination and the bearer SubjectConfirmationData Recipient are this service
 *     provider's assertion consumer URL;</li>
 *     <li>the Response and Assertion Issuer are the configured IdP;</li>
 *     <li>the Assertion is addressed to this service provider (AudienceRestriction) and is inside its
 *     Conditions NotBefore / NotOnOrAfter window;</li>
 *     <li>there is a bearer SubjectConfirmation with an unexpired NotOnOrAfter whose InResponseTo matches
 *     the Response;</li>
 *     <li>a solicited Response answers an authentication request started from this browser, and an
 *     unsolicited one is only accepted when {@link SamlConstants#ALLOW_UNSOLICITED_RESPONSES} is on;</li>
 *     <li>the Assertion has an AuthnStatement whose IdP session has not ended;</li>
 *     <li>the Assertion has not been used before, on any node ({@link AssertionReplayStore}, until its
 *     confirmation expires).</li>
 * </ul>
 *
 * OpenSAML 3.3.1 does ship {@code SAML20AssertionValidator}, but not the parameters for valid issuers or the
 * expected InResponseTo ({@code VALID_ISSUERS}, {@code SC_VALID_IN_RESPONSE_TO}, both added in 4.0). It also
 * treats a missing Conditions, AudienceRestriction, Recipient or NotOnOrAfter as valid, and resolves
 * {@code SubjectConfirmationData@Address} through DNS. So the checks are implemented here explicitly.
 *
 * @author dotCMS
 */
public class SamlResponseValidator {

    private static final String API_PATH_PREFIX = "/api/v1";
    private static final int MAX_LOGGED_VALUE_LENGTH = 256;

    private final EndpointService endpointService;
    private final MetaDataService metaDataService;
    private final SamlConfigurationService samlConfigurationService;
    private final MessageObserver messageObserver;
    private final AssertionReplayStore replayStore;
    private final Clock clock;

    public SamlResponseValidator(final EndpointService endpointService,
                                 final MetaDataService metaDataService,
                                 final SamlConfigurationService samlConfigurationService,
                                 final MessageObserver messageObserver) {

        this(endpointService, metaDataService, samlConfigurationService, messageObserver,
                new DatabaseAssertionReplayStore(), Clock.systemUTC());
    }

    /**
     * For tests and custom deployments: a specific replay store and clock.
     */
    public SamlResponseValidator(final EndpointService endpointService,
                                 final MetaDataService metaDataService,
                                 final SamlConfigurationService samlConfigurationService,
                                 final MessageObserver messageObserver,
                                 final AssertionReplayStore replayStore,
                                 final Clock clock) {

        this.endpointService          = endpointService;
        this.metaDataService          = metaDataService;
        this.samlConfigurationService = samlConfigurationService;
        this.messageObserver          = messageObserver;
        this.replayStore              = replayStore;
        this.clock                    = clock;
    }

    /**
     * Validates the response and its (already decrypted and signature-verified) assertion. Throws a
     * {@link SamlException} on the first check that fails.
     *
     * @param response                      {@link Response}
     * @param assertion                     {@link Assertion} taken from the response
     * @param request                       {@link HttpServletRequest} carrying the response
     * @param httpServletResponse           {@link HttpServletResponse}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     */
    public void validate(final Response response, final Assertion assertion,
                         final HttpServletRequest request, final HttpServletResponse httpServletResponse,
                         final IdentityProviderConfiguration identityProviderConfiguration) {

        final long now         = this.clock.millis();
        final long clockSkew   = this.getClockSkew(identityProviderConfiguration);
        final Set<String> acsUrls = this.getAssertionConsumerUrls(identityProviderConfiguration);
        final String idpEntityId  = this.getIdentityProviderEntityId(identityProviderConfiguration);
        final String requestId    = StringUtils.trimToNull(response.getInResponseTo());

        this.validateDestination(response, acsUrls, identityProviderConfiguration);
        this.validateIssuer(response.getIssuer(), idpEntityId, "Response",
                response.isSigned() || !response.getEncryptedAssertions().isEmpty(), identityProviderConfiguration);
        this.validateIssuer(assertion.getIssuer(), idpEntityId, "Assertion", true, identityProviderConfiguration);
        this.validateConditions(assertion, now, clockSkew, identityProviderConfiguration);
        final long confirmationExpiry = this.validateSubjectConfirmation(assertion, acsUrls, requestId, now,
                clockSkew, identityProviderConfiguration);
        this.validateAuthnStatements(assertion, now, clockSkew, identityProviderConfiguration);
        this.validateInResponseTo(requestId, request, httpServletResponse, identityProviderConfiguration);
        this.checkReplay(assertion, idpEntityId, confirmationExpiry + clockSkew, identityProviderConfiguration);
    }

    private void validateDestination(final Response response, final Set<String> acsUrls,
                                     final IdentityProviderConfiguration identityProviderConfiguration) {

        final String destination = StringUtils.trimToNull(response.getDestination());
        if (null == destination) {

            // Bindings §3.5.5.2: a signed message MUST carry the Destination it was issued for.
            if (response.isSigned()) {
                throw this.fail(identityProviderConfiguration, "The signed SAML Response has no Destination");
            }
            return;
        }

        if (!matchesAny(destination, acsUrls)) {

            throw this.fail(identityProviderConfiguration, "The SAML Response Destination '" + sanitize(destination)
                    + "' is not this service provider's assertion consumer URL " + acsUrls);
        }
    }

    private void validateIssuer(final Issuer issuer, final String idpEntityId, final String elementName,
                                final boolean required,
                                final IdentityProviderConfiguration identityProviderConfiguration) {

        final String issuerValue = null != issuer ? StringUtils.trimToNull(issuer.getValue()) : null;
        if (null == issuerValue) {

            if (required) {
                throw this.fail(identityProviderConfiguration, "The SAML " + elementName + " has no Issuer");
            }
            return;
        }

        if (!idpEntityId.equals(issuerValue)) {

            throw this.fail(identityProviderConfiguration, "The SAML " + elementName + " Issuer '" + sanitize(issuerValue)
                    + "' is not the configured IdP '" + idpEntityId + "'");
        }
    }

    private void validateConditions(final Assertion assertion, final long now, final long clockSkew,
                                    final IdentityProviderConfiguration identityProviderConfiguration) {

        final Conditions conditions = assertion.getConditions();
        if (null == conditions) {

            throw this.fail(identityProviderConfiguration,
                    "The SAML Assertion has no Conditions, so it is not restricted to this service provider");
        }

        final DateTime notBefore = conditions.getNotBefore();
        if (null != notBefore && now + clockSkew < notBefore.getMillis()) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion is not valid before " + notBefore);
        }

        final DateTime notOnOrAfter = conditions.getNotOnOrAfter();
        if (null != notOnOrAfter && now - clockSkew >= notOnOrAfter.getMillis()) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion expired at " + notOnOrAfter);
        }

        final String spEntityId = StringUtils.trimToNull(identityProviderConfiguration.getSpIssuerURL());
        if (null == spEntityId) {

            throw this.fail(identityProviderConfiguration, "The Service Provider Issuer ID is not configured");
        }

        // Profiles §4.1.4.2: the assertion MUST be restricted to the service provider, and every
        // AudienceRestriction present must include it (Core §2.5.1.4).
        final List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
        if (audienceRestrictions.isEmpty()) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion has no AudienceRestriction");
        }

        for (final AudienceRestriction audienceRestriction : audienceRestrictions) {

            boolean addressedToUs = false;
            for (final Audience audience : audienceRestriction.getAudiences()) {

                if (sameEntityId(audience.getAudienceURI(), spEntityId)) {
                    addressedToUs = true;
                    break;
                }
            }

            if (!addressedToUs) {

                throw this.fail(identityProviderConfiguration,
                        "The SAML Assertion AudienceRestriction does not include this service provider '" + spEntityId + "'");
            }
        }
    }

    /**
     * Returns the NotOnOrAfter (epoch millis) of the first valid bearer confirmation.
     */
    private long validateSubjectConfirmation(final Assertion assertion, final Set<String> acsUrls,
                                             final String requestId, final long now, final long clockSkew,
                                             final IdentityProviderConfiguration identityProviderConfiguration) {

        final Subject subject = assertion.getSubject();
        if (null == subject || subject.getSubjectConfirmations().isEmpty()) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion has no SubjectConfirmation");
        }

        final List<String> problems = new ArrayList<>();
        for (final SubjectConfirmation subjectConfirmation : subject.getSubjectConfirmations()) {

            if (!SubjectConfirmation.METHOD_BEARER.equals(subjectConfirmation.getMethod())) {
                problems.add("method '" + sanitize(subjectConfirmation.getMethod()) + "' is not bearer");
                continue;
            }

            final SubjectConfirmationData data = subjectConfirmation.getSubjectConfirmationData();
            if (null == data) {
                problems.add("no SubjectConfirmationData");
                continue;
            }

            // Profiles §4.1.4.2 says a bearer confirmation must not carry NotBefore. Like OpenSAML, this
            // tolerates one and only checks it is not in the future, for IdPs that send it anyway.
            if (null != data.getNotBefore() && now + clockSkew < data.getNotBefore().getMillis()) {
                problems.add("not valid before " + data.getNotBefore());
                continue;
            }

            if (null == data.getNotOnOrAfter()) {
                problems.add("no NotOnOrAfter");
                continue;
            }

            if (now - clockSkew >= data.getNotOnOrAfter().getMillis()) {
                problems.add("expired at " + data.getNotOnOrAfter());
                continue;
            }

            final String recipient = StringUtils.trimToNull(data.getRecipient());
            if (null == recipient || !matchesAny(recipient, acsUrls)) {
                problems.add("Recipient '" + sanitize(recipient) + "' is not this service provider's assertion consumer URL");
                continue;
            }

            // Profiles §4.1.4.2: the confirmation's InResponseTo must agree with the Response's.
            if (!StringUtils.equals(requestId, StringUtils.trimToNull(data.getInResponseTo()))) {
                problems.add("InResponseTo does not match the SAML Response");
                continue;
            }

            return data.getNotOnOrAfter().getMillis();
        }

        throw this.fail(identityProviderConfiguration,
                "The SAML Assertion has no valid bearer SubjectConfirmation: " + String.join("; ", problems));
    }

    private void validateAuthnStatements(final Assertion assertion, final long now, final long clockSkew,
                                         final IdentityProviderConfiguration identityProviderConfiguration) {

        if (assertion.getAuthnStatements().isEmpty()) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion has no AuthnStatement");
        }

        for (final AuthnStatement authnStatement : assertion.getAuthnStatements()) {

            final DateTime sessionNotOnOrAfter = authnStatement.getSessionNotOnOrAfter();
            if (null != sessionNotOnOrAfter && now - clockSkew >= sessionNotOnOrAfter.getMillis()) {

                throw this.fail(identityProviderConfiguration, "The IdP session in the SAML Assertion ended at " + sessionNotOnOrAfter);
            }
        }
    }

    private void validateInResponseTo(final String requestId, final HttpServletRequest request,
                                      final HttpServletResponse httpServletResponse,
                                      final IdentityProviderConfiguration identityProviderConfiguration) {

        if (null == requestId) {

            if (!this.isUnsolicitedResponseAllowed(identityProviderConfiguration)) {

                throw this.fail(identityProviderConfiguration, "Unsolicited SAML Responses (IdP-initiated login) are not accepted. "
                        + "Start the login from dotCMS, or set '" + SamlConstants.ALLOW_UNSOLICITED_RESPONSES
                        + "=true' on the SAML configuration to allow IdP-initiated login");
            }
            return;
        }

        if (!AuthnRequestStateCookie.consume(request, httpServletResponse, identityProviderConfiguration, requestId)) {

            throw this.fail(identityProviderConfiguration, "The SAML Response InResponseTo '" + sanitize(requestId)
                    + "' does not match an authentication request started from this browser");
        }
    }

    private void checkReplay(final Assertion assertion, final String idpEntityId, final long expiresAt,
                             final IdentityProviderConfiguration identityProviderConfiguration) {

        final String assertionId = StringUtils.trimToNull(assertion.getID());
        if (null == assertionId) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion has no ID");
        }

        final boolean firstUse;
        try {

            firstUse = this.replayStore.markUsed(idpEntityId + '|' + assertionId, expiresAt);
        } catch (SamlException e) {

            throw this.fail(identityProviderConfiguration, "Could not check whether the SAML Assertion '"
                    + sanitize(assertionId) + "' has already been used");
        }

        if (!firstUse) {

            throw this.fail(identityProviderConfiguration, "The SAML Assertion '" + sanitize(assertionId) + "' has already been used");
        }
    }

    private boolean isUnsolicitedResponseAllowed(final IdentityProviderConfiguration identityProviderConfiguration) {

        return identityProviderConfiguration.containsOptionalProperty(SamlConstants.ALLOW_UNSOLICITED_RESPONSES)
                && Boolean.parseBoolean(String.valueOf(identityProviderConfiguration
                        .getOptionalProperty(SamlConstants.ALLOW_UNSOLICITED_RESPONSES)).trim());
    }

    private long getClockSkew(final IdentityProviderConfiguration identityProviderConfiguration) {

        try {

            final Integer clockSkew = this.samlConfigurationService.getConfigAsInteger(identityProviderConfiguration,
                    SamlName.DOT_SAML_CLOCK_SKEW);
            if (null != clockSkew && clockSkew >= 0) {
                return clockSkew.longValue();
            }
        } catch (Exception e) {

            this.messageObserver.updateInfo(this.getClass().getName(),
                    "Optional property not set: " + SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName() + ". Using default.");
        }

        return AssertionResolverHandler.DOT_SAML_CLOCK_SKEW_DEFAULT_VALUE;
    }

    /**
     * The assertion consumer URL published in the SP metadata and AuthnRequest, plus its /api/v1 form
     * (the REST endpoint the public URL is rewritten to), which some IdPs are configured with directly.
     */
    private Set<String> getAssertionConsumerUrls(final IdentityProviderConfiguration identityProviderConfiguration) {

        final Set<String> acsUrls = new LinkedHashSet<>();
        final String acsUrl = this.endpointService.getAssertionConsumerEndpoint(identityProviderConfiguration);
        if (StringUtils.isBlank(acsUrl)) {

            throw this.fail(identityProviderConfiguration, "The assertion consumer URL can not be determined; "
                    + "check the Service Provider Endpoint Hostname");
        }

        acsUrls.add(acsUrl);
        final int pathStart = acsUrl.indexOf(SamlConstants.ASSERTION_CONSUMER_ENDPOINT_DOTSAML3SP);
        if (pathStart > 0) {
            acsUrls.add(acsUrl.substring(0, pathStart) + API_PATH_PREFIX + acsUrl.substring(pathStart));
        }

        return acsUrls;
    }

    private String getIdentityProviderEntityId(final IdentityProviderConfiguration identityProviderConfiguration) {

        final MetaData metaData = this.metaDataService.getMetaData(identityProviderConfiguration);
        final String entityId   = null != metaData ? StringUtils.trimToNull(metaData.getEntityId()) : null;
        if (null == entityId) {

            throw this.fail(identityProviderConfiguration, "The IdP entityID can not be read from the IdP metadata");
        }

        return entityId;
    }

    private SamlException fail(final IdentityProviderConfiguration identityProviderConfiguration, final String reason) {

        final String message = "Rejected SAML Response for IdP '" + identityProviderConfiguration.getIdpName() + "': " + reason;
        this.messageObserver.updateError(this.getClass().getName(), message);
        return new SamlException(message);
    }

    private static boolean matchesAny(final String url, final Set<String> expectedUrls) {

        final String normalized = normalizeUrl(url);
        if (null == normalized) {
            return false;
        }

        for (final String expectedUrl : expectedUrls) {
            if (normalized.equals(normalizeUrl(expectedUrl))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes a URL for comparison: lower-case scheme and host, default port removed, no trailing slash.
     * The path is compared as is. Returns null for anything that is not an absolute http(s) URL.
     */
    static String normalizeUrl(final String url) {

        if (StringUtils.isBlank(url)) {
            return null;
        }

        try {

            final URI uri = new URI(url.trim());
            if (null == uri.getScheme() || null == uri.getHost()) {
                return null;
            }

            final String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme) && !"http".equals(scheme)) {
                return null;
            }

            final int port = uri.getPort();
            final boolean defaultPort = -1 == port || ("https".equals(scheme) && 443 == port) || ("http".equals(scheme) && 80 == port);
            final String path = StringUtils.removeEnd(StringUtils.defaultString(uri.getRawPath()), "/");
            final String query = null != uri.getRawQuery() ? "?" + uri.getRawQuery() : StringUtils.EMPTY;

            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? StringUtils.EMPTY : ":" + port)
                    + path + query;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean sameEntityId(final String audience, final String spEntityId) {

        final String trimmedAudience = StringUtils.trimToNull(audience);
        return null != trimmedAudience
                && StringUtils.removeEnd(trimmedAudience, "/").equals(StringUtils.removeEnd(spEntityId, "/"));
    }

    private static String sanitize(final String value) {

        if (null == value) {
            return "null";
        }

        final String singleLine = value.replaceAll("[\\p{Cntrl}]", "_");
        return singleLine.length() > MAX_LOGGED_VALUE_LENGTH ? singleLine.substring(0, MAX_LOGGED_VALUE_LENGTH) + "..." : singleLine;
    }
}
