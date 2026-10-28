package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.MetaData;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.init.SamlInitializer;
import com.dotcms.saml.service.internal.EndpointService;
import com.dotcms.saml.service.internal.MetaDataService;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.security.credential.Credential;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Covers the SAML 2.0 Web Browser SSO profile checks done by {@link SamlResponseValidator}.
 */
public class TestSamlResponseValidator {

    private static final String IDP_ENTITY_ID = "https://idp.example.com/metadata";
    private static final String SP_ENTITY_ID  = "https://dotcms.example.com/dotAdmin";
    private static final String CONFIG_ID     = "48190c8c-42c4-46af-8d1a-0cd5db894797";
    private static final String ACS_URL       = "https://dotcms.example.com/dotsaml/login/" + CONFIG_ID;
    // real time: the replay cache storage expires entries against the system clock
    private static final long   NOW           = System.currentTimeMillis();

    private static XMLObjectBuilderFactory builderFactory;

    private Map<String, Object> optionalProperties;
    private SamlResponseValidator validator;

    @BeforeClass
    public static void initOpenSaml() {

        new SamlInitializer().init(Collections.emptyMap());
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
    }

    @Before
    public void setUp() {

        this.optionalProperties = new HashMap<>();
        this.validator = newValidator(NOW);
    }

    @Test
    public void validSolicitedResponseIsAccepted() {

        final String requestId = newId();
        final List<String> setCookies = new ArrayList<>();

        this.validator.validate(response(requestId), assertion(requestId), requestWithState(requestId),
                responseCapturing(setCookies), idp());

        Assert.assertTrue("the outstanding request must be cleared",
                setCookies.stream().anyMatch(cookie -> cookie.startsWith(AuthnRequestStateCookie.COOKIE_PREFIX + CONFIG_ID + "=;")
                        && cookie.contains("Max-Age=0")));
    }

    @Test
    public void apiV1AssertionConsumerUrlIsAccepted() {

        final String requestId = newId();
        final String apiAcsUrl = "https://dotcms.example.com/api/v1/dotsaml/login/" + CONFIG_ID;
        final Response response = response(requestId);
        response.setDestination(apiAcsUrl);
        final Assertion assertion = assertion(requestId);
        assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setRecipient(apiAcsUrl);

        this.validator.validate(response, assertion, requestWithState(requestId), responseCapturing(new ArrayList<>()), idp());
    }

    @Test
    public void replayedAssertionIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        this.validator.validate(response(requestId), assertion, requestWithState(requestId), responseCapturing(new ArrayList<>()), idp());

        assertRejected("has already been used", () -> this.validator.validate(response(requestId), assertion,
                requestWithState(requestId), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void unavailableReplayStoreRejectsTheResponse() {

        final SamlResponseValidator failingStore = new SamlResponseValidator(endpointService(), metaDataService(),
                new MockSamlConfigurationService(), new MockMessageObserver(),
                (key, expiresAt) -> { throw new SamlException("database down"); },
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC));
        final String requestId = newId();

        assertRejected("Could not check whether the SAML Assertion", () -> failingStore.validate(response(requestId),
                assertion(requestId), requestWithState(requestId), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void assertionForAnotherServiceProviderIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0)
                .setAudienceURI("https://partner-app.example.com");

        assertRejected("AudienceRestriction does not include this service provider", () -> validate(requestId, assertion));
    }

    @Test
    public void everyAudienceRestrictionMustIncludeTheServiceProvider() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getConditions().getAudienceRestrictions().add(audienceRestriction("https://partner-app.example.com"));

        assertRejected("AudienceRestriction does not include this service provider", () -> validate(requestId, assertion));
    }

    @Test
    public void assertionWithoutAudienceRestrictionIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getConditions().getAudienceRestrictions().clear();

        assertRejected("no AudienceRestriction", () -> validate(requestId, assertion));
    }

    @Test
    public void assertionWithoutConditionsIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.setConditions(null);

        assertRejected("has no Conditions", () -> validate(requestId, assertion));
    }

    @Test
    public void expiredConditionsAreRejectedEvenWhenTheResponseIssueInstantIsFresh() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        // the assertion's own validity window applies whatever the Response IssueInstant says
        assertion.getConditions().setNotOnOrAfter(new DateTime(NOW - 60_000));

        assertRejected("expired at", () -> validate(requestId, assertion));
    }

    @Test
    public void notYetValidConditionsAreRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getConditions().setNotBefore(new DateTime(NOW + 60_000));

        assertRejected("not valid before", () -> validate(requestId, assertion));
    }

    @Test
    public void expiredSubjectConfirmationIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        confirmationData(assertion).setNotOnOrAfter(new DateTime(NOW - 60_000));

        assertRejected("no valid bearer SubjectConfirmation", () -> validate(requestId, assertion));
    }

    @Test
    public void subjectConfirmationWithoutNotOnOrAfterIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        confirmationData(assertion).setNotOnOrAfter(null);

        assertRejected("no NotOnOrAfter", () -> validate(requestId, assertion));
    }

    @Test
    public void subjectConfirmationForAnotherRecipientIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        confirmationData(assertion).setRecipient("https://partner-app.example.com/acs");

        assertRejected("is not this service provider's assertion consumer URL", () -> validate(requestId, assertion));
    }

    @Test
    public void nonBearerSubjectConfirmationIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getSubject().getSubjectConfirmations().get(0).setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);

        assertRejected("is not bearer", () -> validate(requestId, assertion));
    }

    @Test
    public void wrongDestinationIsRejected() {

        final String requestId = newId();
        final Response response = response(requestId);
        response.setDestination("https://partner-app.example.com/acs");

        assertRejected("Destination", () -> this.validator.validate(response, assertion(requestId),
                requestWithState(requestId), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void assertionFromAnotherIssuerIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getIssuer().setValue("https://other-idp.example.com");

        assertRejected("is not the configured IdP", () -> validate(requestId, assertion));
    }

    @Test
    public void assertionWithoutIssuerIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.setIssuer(null);

        assertRejected("Assertion has no Issuer", () -> validate(requestId, assertion));
    }

    @Test
    public void responseFromAnotherIssuerIsRejected() {

        final String requestId = newId();
        final Response response = response(requestId);
        response.getIssuer().setValue("https://other-idp.example.com");

        assertRejected("Response Issuer", () -> this.validator.validate(response, assertion(requestId),
                requestWithState(requestId), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void responseNotStartedFromThisBrowserIsRejected() {

        final String requestId = newId();

        assertRejected("does not match an authentication request started from this browser",
                () -> this.validator.validate(response(requestId), assertion(requestId),
                        requestWithState(newId()), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void requestStateForAnotherIdpConfigurationIsRejected() {

        final String requestId = newId();
        // the request was recorded for a different IdP configuration
        final HttpServletRequest request = request(new Cookie(
                AuthnRequestStateCookie.COOKIE_PREFIX + "8a7d5e23-da1e-420a-b4f0-471e7da8ea2d", stateEntry(requestId)));

        assertRejected("does not match an authentication request started from this browser",
                () -> this.validator.validate(response(requestId), assertion(requestId), request,
                        responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void unsolicitedResponseIsRejectedByDefault() {

        assertRejected("Unsolicited SAML Responses", () -> this.validator.validate(response(null), assertion(null),
                request(), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void unsolicitedResponseIsAcceptedWhenAllowed() {

        this.optionalProperties.put(SamlConstants.ALLOW_UNSOLICITED_RESPONSES, "true");

        this.validator.validate(response(null), assertion(null), request(), responseCapturing(new ArrayList<>()), idp());
    }

    @Test
    public void confirmationInResponseToMustMatchTheResponse() {

        this.optionalProperties.put(SamlConstants.ALLOW_UNSOLICITED_RESPONSES, "true");
        final String requestId = newId();

        // Response without InResponseTo, confirmation answering a request
        assertRejected("InResponseTo does not match the SAML Response", () -> this.validator.validate(response(null),
                assertion(requestId), request(), responseCapturing(new ArrayList<>()), idp()));
    }

    @Test
    public void assertionWithoutAuthnStatementIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getAuthnStatements().clear();

        assertRejected("no AuthnStatement", () -> validate(requestId, assertion));
    }

    @Test
    public void endedIdpSessionIsRejected() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        assertion.getAuthnStatements().get(0).setSessionNotOnOrAfter(new DateTime(NOW - 60_000));

        assertRejected("IdP session", () -> validate(requestId, assertion));
    }

    @Test
    public void clockSkewIsTolerated() {

        final String requestId = newId();
        final Assertion assertion = assertion(requestId);
        confirmationData(assertion).setNotOnOrAfter(new DateTime(NOW - 500)); // default skew is 1000 ms

        validate(requestId, assertion);
    }

    @Test
    public void normalizeUrlIgnoresCaseOfHostDefaultPortAndTrailingSlash() {

        Assert.assertEquals(SamlResponseValidator.normalizeUrl("https://dotcms.example.com/dotsaml/login/1"),
                SamlResponseValidator.normalizeUrl("HTTPS://DotCMS.Example.com:443/dotsaml/login/1/"));
        Assert.assertNotEquals(SamlResponseValidator.normalizeUrl("https://dotcms.example.com/dotsaml/login/1"),
                SamlResponseValidator.normalizeUrl("https://dotcms.example.com:8443/dotsaml/login/1"));
        Assert.assertNull(SamlResponseValidator.normalizeUrl("javascript:alert(1)"));
        Assert.assertNull(SamlResponseValidator.normalizeUrl("/dotsaml/login/1"));
    }

    // ---------------------------------------------------------------------------------------------------------

    private void validate(final String requestId, final Assertion assertion) {

        this.validator.validate(response(requestId), assertion, requestWithState(requestId),
                responseCapturing(new ArrayList<>()), idp());
    }

    private static void assertRejected(final String expectedReason, final Runnable validation) {

        try {
            validation.run();
            Assert.fail("Expected the SAML Response to be rejected: " + expectedReason);
        } catch (SamlException e) {
            Assert.assertTrue("Unexpected reason: " + e.getMessage(), e.getMessage().contains(expectedReason));
        }
    }

    private SamlResponseValidator newValidator(final long now) {

        return new SamlResponseValidator(endpointService(), metaDataService(), new MockSamlConfigurationService(),
                new MockMessageObserver(), new InMemoryAssertionReplayStore(),
                Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC));
    }

    private static String newId() {

        return "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Response response(final String inResponseTo) {

        final Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID(newId());
        response.setIssueInstant(new DateTime(NOW));
        response.setDestination(ACS_URL);
        response.setInResponseTo(inResponseTo);
        response.setIssuer(issuer(IDP_ENTITY_ID));
        return response;
    }

    private static Assertion assertion(final String inResponseTo) {

        final Assertion assertion = build(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(newId());
        assertion.setIssueInstant(new DateTime(NOW));
        assertion.setIssuer(issuer(IDP_ENTITY_ID));

        final NameID nameID = build(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("user@example.com");

        final SubjectConfirmationData data = build(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        data.setRecipient(ACS_URL);
        data.setNotOnOrAfter(new DateTime(NOW + 300_000));
        data.setInResponseTo(inResponseTo);

        final SubjectConfirmation subjectConfirmation = build(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subjectConfirmation.setSubjectConfirmationData(data);

        final Subject subject = build(Subject.DEFAULT_ELEMENT_NAME);
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        assertion.setSubject(subject);

        final Conditions conditions = build(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(new DateTime(NOW - 5_000));
        conditions.setNotOnOrAfter(new DateTime(NOW + 300_000));
        conditions.getAudienceRestrictions().add(audienceRestriction(SP_ENTITY_ID));
        assertion.setConditions(conditions);

        final AuthnStatement authnStatement = build(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(new DateTime(NOW));
        authnStatement.setSessionNotOnOrAfter(new DateTime(NOW + 3_600_000));
        assertion.getAuthnStatements().add(authnStatement);

        return assertion;
    }

    private static AudienceRestriction audienceRestriction(final String audienceUri) {

        final Audience audience = build(Audience.DEFAULT_ELEMENT_NAME);
        audience.setAudienceURI(audienceUri);
        final AudienceRestriction audienceRestriction = build(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        audienceRestriction.getAudiences().add(audience);
        return audienceRestriction;
    }

    private static SubjectConfirmationData confirmationData(final Assertion assertion) {

        return assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData();
    }

    private static Issuer issuer(final String value) {

        final Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(value);
        return issuer;
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(final QName elementName) {

        return (T) builderFactory.getBuilder(elementName).buildObject(elementName);
    }

    private static HttpServletRequest requestWithState(final String requestId) {

        return request(new Cookie(AuthnRequestStateCookie.COOKIE_PREFIX + CONFIG_ID, stateEntry(requestId)));
    }

    private static String stateEntry(final String requestId) {

        return requestId + ":" + (System.currentTimeMillis() / 1000L);
    }

    private static HttpServletRequest request(final Cookie... cookies) {

        return (HttpServletRequest) Proxy.newProxyInstance(TestSamlResponseValidator.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> "getCookies".equals(method.getName()) ? cookies : null);
    }

    private static HttpServletResponse responseCapturing(final List<String> setCookies) {

        return (HttpServletResponse) Proxy.newProxyInstance(TestSamlResponseValidator.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("addHeader".equals(method.getName()) && "Set-Cookie".equals(args[0])) {
                        setCookies.add((String) args[1]);
                    }
                    return null;
                });
    }

    private IdentityProviderConfiguration idp() {

        final Map<String, Object> properties = this.optionalProperties;
        return (IdentityProviderConfiguration) Proxy.newProxyInstance(TestSamlResponseValidator.class.getClassLoader(),
                new Class<?>[]{IdentityProviderConfiguration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":                    return CONFIG_ID;
                        case "getIdpName":               return "Test IdP";
                        case "getSpIssuerURL":           return SP_ENTITY_ID;
                        case "getSpEndpointHostname":    return "dotcms.example.com";
                        case "getSignatureValidationType": return SamlConstants.ASSERTION;
                        case "isEnabled":                return true;
                        case "containsOptionalProperty": return properties.containsKey((String) args[0]);
                        case "getOptionalProperty":      return properties.get((String) args[0]);
                        default:                         return null;
                    }
                });
    }

    private static EndpointService endpointService() {

        return new EndpointService() {
            @Override public String getAssertionConsumerEndpoint(final IdentityProviderConfiguration idp) { return ACS_URL; }
            @Override public String getSingleLogoutEndpoint(final IdentityProviderConfiguration idp) { return null; }
            @Override public String[] getAccessFilterArray(final IdentityProviderConfiguration idp) { return null; }
            @Override public String[] getLogoutPathArray(final IdentityProviderConfiguration idp) { return null; }
            @Override public String[] getIncludePathArray(final IdentityProviderConfiguration idp) { return null; }
        };
    }

    private static MetaDataService metaDataService() {

        final MetaData metaData = new MetaData(IDP_ENTITY_ID, null, Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList());
        return new MetaDataService() {
            @Override public MetaData getMetaData(final IdentityProviderConfiguration idp) { return metaData; }
            @Override public MetaDescriptorService getMetaDescriptorService(final IdentityProviderConfiguration idp) { return null; }
            @Override public Collection<Credential> getSigningCredentials(final IdentityProviderConfiguration idp) { return null; }
            @Override public String getIdentityProviderDestinationSSOURL(final IdentityProviderConfiguration idp) { return null; }
            @Override public String getIdentityProviderDestinationSLOURL(final IdentityProviderConfiguration idp) { return null; }
        };
    }
}
