package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.external.MetaData;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.CredentialServiceImpl;
import com.dotcms.saml.service.impl.InMemoryAssertionReplayStore;
import com.dotcms.saml.service.impl.MockMessageObserver;
import com.dotcms.saml.service.impl.MockSamlConfigurationService;
import com.dotcms.saml.service.impl.SamlCoreServiceImpl;
import com.dotcms.saml.service.impl.SamlResponseValidator;
import com.dotcms.saml.service.init.SamlInitializer;
import com.dotcms.saml.service.internal.CredentialProvider;
import com.dotcms.saml.service.internal.EndpointService;
import com.dotcms.saml.service.internal.MetaDataService;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * End to end through the HTTP-POST assertion consumer handler: decoding, signature rules and the profile
 * checks.
 */
public class TestHttpPostAssertionResolverHandler {

    private static final String IDP_ENTITY_ID = "https://idp.example.com/metadata";
    private static final String SP_ENTITY_ID  = "https://dotcms.example.com/dotAdmin";
    private static final String CONFIG_ID     = "48190c8c-42c4-46af-8d1a-0cd5db894797";
    private static final String ACS_URL       = "https://dotcms.example.com/dotsaml/login/" + CONFIG_ID;

    private static XMLObjectBuilderFactory builderFactory;
    private static Credential idpCredential;
    private static Credential spCredential;

    @BeforeClass
    public static void initOpenSaml() throws Exception {

        new SamlInitializer().init(Collections.emptyMap());
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final KeyPair keyPair = KeySupport.generateKeyPair("RSA", 2048, null);
        idpCredential = CredentialSupport.getSimpleCredential(keyPair.getPublic(), keyPair.getPrivate());
        final KeyPair spKeyPair = KeySupport.generateKeyPair("RSA", 2048, null);
        spCredential = CredentialSupport.getSimpleCredential(spKeyPair.getPublic(), spKeyPair.getPrivate());
    }

    @Test
    public void unsignedResponseIsRejectedWhenValidationTypeIsNone() throws Exception {

        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.NONE, false));

        assertRejected("must be signed", () -> handler().resolveAssertion(post(samlResponse, requestId),
                response(), idp("none")));
    }

    @Test
    public void validSignedAssertionIsResolved() throws Exception {

        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.ASSERTION, false));

        final Assertion assertion = handler().resolveAssertion(post(samlResponse, requestId), response(),
                idp(SamlConstants.ASSERTION));
        Assert.assertEquals("admin@example.com", assertion.getSubject().getNameID().getValue());
    }

    @Test
    public void responseSignedIdpWithPlainAssertionIsResolved() throws Exception {

        // e.g. Entra ID set to sign the SAML response only
        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.RESPONSE, false));

        final Assertion assertion = handler().resolveAssertion(post(samlResponse, requestId), response(),
                idp(SamlConstants.RESPONSE));
        Assert.assertEquals("admin@example.com", assertion.getSubject().getNameID().getValue());
    }

    @Test
    public void responseSignedIdpWithEncryptedAssertionIsResolved() throws Exception {

        // Shibboleth's default: signed response, encrypted (unsigned) assertion
        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.RESPONSE, true));

        final Assertion assertion = handler(encryptedAssertionConfiguration()).resolveAssertion(
                post(samlResponse, requestId), response(), idp(SamlConstants.RESPONSE));
        Assert.assertEquals("admin@example.com", assertion.getSubject().getNameID().getValue());
    }

    @Test
    public void responseAndAssertionSignedIdpIsResolvedWhicheverPartIsRequired() throws Exception {

        for (final String validationType : new String[]{SamlConstants.RESPONSE_AND_ASSERTION,
                SamlConstants.RESPONSE, SamlConstants.ASSERTION}) {

            final String requestId = newId();
            final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.BOTH, false));

            final Assertion assertion = handler().resolveAssertion(post(samlResponse, requestId), response(),
                    idp(validationType));
            Assert.assertEquals(validationType, "admin@example.com", assertion.getSubject().getNameID().getValue());
        }
    }

    @Test
    public void responseOnlySignatureIsRejectedWhenTheAssertionMustBeSigned() throws Exception {

        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.RESPONSE, false));

        assertRejected("Assertion for IdP 'Test IdP' must be signed", () -> handler().resolveAssertion(
                post(samlResponse, requestId), response(), idp(SamlConstants.RESPONSE_AND_ASSERTION)));
    }

    @Test
    public void alteredSignedResponseIsRejected() throws Exception {

        final String requestId = newId();
        final String xml = SerializeSupport.nodeToString(
                response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.RESPONSE, false).getDOM());
        final String altered = Base64.getEncoder().encodeToString(
                xml.replace("admin@example.com", "root@example.com").getBytes(StandardCharsets.UTF_8));

        assertRejected("Response Signature cannot be validated", () -> handler().resolveAssertion(
                post(altered, requestId), response(), idp(SamlConstants.RESPONSE)));
    }

    @Test
    public void validlySignedAssertionForAnotherServiceProviderIsRejected() throws Exception {

        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, "https://partner-app.example.com"),
                Signing.ASSERTION, false));

        assertRejected("AudienceRestriction does not include this service provider",
                () -> handler().resolveAssertion(post(samlResponse, requestId), response(), idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void samlMessageThatIsNotAResponseIsRejected() throws Exception {

        final LogoutResponse logoutResponse = build(LogoutResponse.DEFAULT_ELEMENT_NAME);
        logoutResponse.setID(newId());
        logoutResponse.setIssueInstant(new DateTime());
        logoutResponse.setIssuer(issuer());
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(logoutResponse).marshall(logoutResponse);
        final String posted = Base64.getEncoder().encodeToString(
                SerializeSupport.nodeToString(logoutResponse.getDOM()).getBytes(StandardCharsets.UTF_8));

        assertRejected("is not a Response", () -> handler().resolveAssertion(post(posted, newId()), response(),
                idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void handlerWithoutValidatorFailsClosed() throws Exception {

        final String requestId = newId();
        final String samlResponse = encode(response(requestId, assertion(requestId, SP_ENTITY_ID), Signing.ASSERTION, false));
        final MockSamlConfigurationService configurationService = new MockSamlConfigurationService();
        final HttpPostAssertionResolverHandlerImpl handler = new HttpPostAssertionResolverHandlerImpl(
                new MockMessageObserver(), samlCoreService(configurationService), configurationService, null);

        assertRejected("validator is not available",
                () -> handler.resolveAssertion(post(samlResponse, requestId), response(), idp(SamlConstants.ASSERTION)));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void assertRejected(final String expectedReason, final Runnable resolution) {

        try {
            resolution.run();
            Assert.fail("Expected the SAML Response to be rejected: " + expectedReason);
        } catch (SamlException e) {
            Assert.assertTrue("Unexpected reason: " + e.getMessage(), e.getMessage().contains(expectedReason));
        }
    }

    private enum Signing { NONE, ASSERTION, RESPONSE, BOTH }

    private static HttpPostAssertionResolverHandlerImpl handler() {

        return handler(new MockSamlConfigurationService());
    }

    private static HttpPostAssertionResolverHandlerImpl handler(final MockSamlConfigurationService configurationService) {

        return new HttpPostAssertionResolverHandlerImpl(new MockMessageObserver(), samlCoreService(configurationService),
                configurationService, new SamlResponseValidator(endpointService(), metaDataService(), configurationService,
                        new MockMessageObserver(), new InMemoryAssertionReplayStore(), Clock.systemUTC()));
    }

    private static SamlCoreServiceImpl samlCoreService(final MockSamlConfigurationService configurationService) {

        return new SamlCoreServiceImpl(new CredentialServiceImpl(configurationService), endpointService(),
                metaDataService(), new MockMessageObserver(), configurationService, null);
    }

    /** Assertions arrive encrypted for the SP key held by {@link TestSpCredentialProvider}. */
    private static MockSamlConfigurationService encryptedAssertionConfiguration() {

        return new MockSamlConfigurationService() {

            @Override
            public Boolean getConfigAsBoolean(final IdentityProviderConfiguration idp, final SamlName samlName) {
                return SamlName.DOTCMS_SAML_IS_ASSERTION_ENCRYPTED == samlName || super.getConfigAsBoolean(idp, samlName);
            }

            @Override
            public String getConfigAsString(final IdentityProviderConfiguration idp, final SamlName samlName) {
                return SamlName.DOT_SAML_SERVICE_PROVIDER_CUSTOM_CREDENTIAL_PROVIDER_CLASSNAME == samlName
                        ? TestSpCredentialProvider.class.getName() : super.getConfigAsString(idp, samlName);
            }
        };
    }

    /** The SP decryption key, loaded by name the way a custom credential provider is configured. */
    public static class TestSpCredentialProvider implements CredentialProvider {

        @Override
        public Credential createCredential() {
            return spCredential;
        }
    }

    private static String encode(final Response response) {

        // no pretty printing: whitespace inside the signed assertion is covered by its digest
        return Base64.getEncoder().encodeToString(
                SerializeSupport.nodeToString(response.getDOM()).getBytes(StandardCharsets.UTF_8));
    }

    private static Response response(final String requestId, final Assertion assertion, final Signing signing,
                                     final boolean encryptAssertion) throws Exception {

        final Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID(newId());
        response.setIssueInstant(new DateTime());
        response.setDestination(ACS_URL);
        response.setInResponseTo(requestId);
        response.setIssuer(issuer());

        final StatusCode statusCode = build(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(StatusCode.SUCCESS);
        final Status status = build(Status.DEFAULT_ELEMENT_NAME);
        status.setStatusCode(statusCode);
        response.setStatus(status);

        final boolean signAssertion = Signing.ASSERTION == signing || Signing.BOTH == signing;
        final boolean signResponse  = Signing.RESPONSE == signing || Signing.BOTH == signing;
        final Signature assertionSignature = signAssertion ? signature() : null;
        final Signature responseSignature  = signResponse ? signature() : null;
        assertion.setSignature(assertionSignature);
        response.setSignature(responseSignature);

        if (encryptAssertion) {

            // the assertion is signed (if at all) before it is encrypted
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
            if (null != assertionSignature) {
                Signer.signObject(assertionSignature);
            }
            response.getEncryptedAssertions().add(encrypt(assertion));
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(response).marshall(response);
        } else {

            response.getAssertions().add(assertion);
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(response).marshall(response);
            if (null != assertionSignature) {
                Signer.signObject(assertionSignature);
            }
        }

        // the response signature covers the assertion, so it is computed last
        if (null != responseSignature) {
            Signer.signObject(responseSignature);
        }

        return response;
    }

    private static Signature signature() {

        final Signature signature = build(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(idpCredential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        return signature;
    }

    private static EncryptedAssertion encrypt(final Assertion assertion) throws Exception {

        final DataEncryptionParameters dataParameters = new DataEncryptionParameters();
        dataParameters.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128);
        final KeyEncryptionParameters keyParameters = new KeyEncryptionParameters();
        keyParameters.setEncryptionCredential(spCredential);
        keyParameters.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        final Encrypter encrypter = new Encrypter(dataParameters, keyParameters);
        encrypter.setKeyPlacement(Encrypter.KeyPlacement.INLINE);
        return encrypter.encrypt(assertion);
    }

    private static Assertion assertion(final String requestId, final String audienceUri) {

        final long now = System.currentTimeMillis();
        final Assertion assertion = build(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(newId());
        assertion.setIssueInstant(new DateTime(now));
        assertion.setIssuer(issuer());

        final NameID nameID = build(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("admin@example.com");

        final SubjectConfirmationData data = build(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        data.setRecipient(ACS_URL);
        data.setNotOnOrAfter(new DateTime(now + 300_000));
        data.setInResponseTo(requestId);

        final SubjectConfirmation subjectConfirmation = build(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subjectConfirmation.setSubjectConfirmationData(data);

        final Subject subject = build(Subject.DEFAULT_ELEMENT_NAME);
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        assertion.setSubject(subject);

        final Audience audience = build(Audience.DEFAULT_ELEMENT_NAME);
        audience.setAudienceURI(audienceUri);
        final AudienceRestriction audienceRestriction = build(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        audienceRestriction.getAudiences().add(audience);
        final Conditions conditions = build(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(new DateTime(now - 5_000));
        conditions.setNotOnOrAfter(new DateTime(now + 300_000));
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        final AuthnStatement authnStatement = build(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(new DateTime(now));
        assertion.getAuthnStatements().add(authnStatement);

        return assertion;
    }

    private static Issuer issuer() {

        final Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(IDP_ENTITY_ID);
        return issuer;
    }

    private static String newId() {

        return "_" + UUID.randomUUID().toString().replace("-", "");
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(final QName elementName) {

        return (T) builderFactory.getBuilder(elementName).buildObject(elementName);
    }

    private static HttpServletRequest post(final String samlResponse, final String requestId) {

        final Cookie[] cookies = {new Cookie(AuthnRequestStateCookie.COOKIE_PREFIX + CONFIG_ID,
                requestId + ":" + (System.currentTimeMillis() / 1000L))};
        return (HttpServletRequest) Proxy.newProxyInstance(TestHttpPostAssertionResolverHandler.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMethod":    return "POST";
                        case "getCookies":   return cookies;
                        case "getParameter": return "SAMLResponse".equals(args[0]) ? samlResponse : null;
                        default:             return null;
                    }
                });
    }

    private static HttpServletResponse response() {

        return (HttpServletResponse) Proxy.newProxyInstance(TestHttpPostAssertionResolverHandler.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> null);
    }

    private static IdentityProviderConfiguration idp(final String signatureValidationType) {

        return (IdentityProviderConfiguration) Proxy.newProxyInstance(TestHttpPostAssertionResolverHandler.class.getClassLoader(),
                new Class<?>[]{IdentityProviderConfiguration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":                      return CONFIG_ID;
                        case "getIdpName":                 return "Test IdP";
                        case "getSpIssuerURL":             return SP_ENTITY_ID;
                        case "getSpEndpointHostname":      return "dotcms.example.com";
                        case "getSignatureValidationType": return signatureValidationType;
                        case "isEnabled":                  return true;
                        case "containsOptionalProperty":   return false;
                        default:                           return null;
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
                Collections.singletonList(idpCredential));
        return new MetaDataService() {
            @Override public MetaData getMetaData(final IdentityProviderConfiguration idp) { return metaData; }
            @Override public MetaDescriptorService getMetaDescriptorService(final IdentityProviderConfiguration idp) { return null; }
            @Override public Collection<Credential> getSigningCredentials(final IdentityProviderConfiguration idp) {
                return Collections.singletonList(idpCredential);
            }
            @Override public String getIdentityProviderDestinationSSOURL(final IdentityProviderConfiguration idp) { return null; }
            @Override public String getIdentityProviderDestinationSLOURL(final IdentityProviderConfiguration idp) { return null; }
        };
    }
}
