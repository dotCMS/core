package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.MetaData;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.init.SamlInitializer;
import com.dotcms.saml.service.internal.MetaDataService;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;

import javax.xml.namespace.QName;
import java.lang.reflect.Proxy;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Signature rules: an unrecognized validation type fails closed, a required signature must be present, and
 * a signature that is present is always verified (it is never rejected just for being there).
 */
public class TestSamlSignatureVerification {

    private static XMLObjectBuilderFactory builderFactory;
    private static Credential idpCredential;
    private static Credential otherCredential;

    @BeforeClass
    public static void initOpenSaml() throws Exception {

        new SamlInitializer().init(Collections.emptyMap());
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        idpCredential   = newCredential();
        otherCredential = newCredential();
    }

    @Test
    public void unrecognizedValidationTypesFailClosed() {

        for (final String configured : new String[]{"none", "None", "", null, "signature"}) {

            Assert.assertEquals("type '" + configured + "'", SamlConstants.RESPONSE_AND_ASSERTION,
                    CredentialServiceImpl.resolveSignatureValidationType(idp(configured)));
        }

        Assert.assertEquals(SamlConstants.RESPONSE_AND_ASSERTION,
                CredentialServiceImpl.resolveSignatureValidationType(idp("responseAndAssertion ")));
        Assert.assertEquals(SamlConstants.ASSERTION, CredentialServiceImpl.resolveSignatureValidationType(idp(" Assertion")));
        Assert.assertEquals(SamlConstants.RESPONSE, CredentialServiceImpl.resolveSignatureValidationType(idp("response")));
    }

    @Test
    public void signatureChecksCanNotBeTurnedOff() {

        // MockSamlConfigurationService answers false for every boolean, i.e. verify.signature.* = false
        final CredentialServiceImpl credentialService = new CredentialServiceImpl(new MockSamlConfigurationService());

        Assert.assertTrue(credentialService.isVerifySignatureCredentialsNeeded(idp(SamlConstants.ASSERTION)));
        Assert.assertTrue(credentialService.isVerifySignatureProfileNeeded(idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void unsignedResponseAndAssertionAreRejectedWhenValidationTypeIsNone() {

        final SamlCoreServiceImpl samlCoreService = samlCoreService();
        final IdentityProviderConfiguration idp = idp("none");

        assertRejected("must be signed", () -> samlCoreService.verifyResponseSignature(response(), idp));
        assertRejected("must be signed", () -> samlCoreService.verifyAssertionSignature(assertion(), idp));
    }

    @Test
    public void unsignedAssertionIsRejectedWhenItsSignatureIsRequired() {

        assertRejected("must be signed",
                () -> samlCoreService().verifyAssertionSignature(assertion(), idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void unsignedAssertionIsAcceptedWhenTheResponseSignatureCoversIt() {

        samlCoreService().verifyAssertionSignature(assertion(), idp(SamlConstants.RESPONSE));
    }

    @Test
    public void validAssertionSignatureIsAccepted() throws Exception {

        samlCoreService().verifyAssertionSignature(signedAssertion(idpCredential), idp(SamlConstants.ASSERTION));
    }

    @Test
    public void presentAssertionSignatureIsVerifiedEvenWhenNotRequired() throws Exception {

        // previously a signed assertion was rejected outright in "response" mode; now it is verified
        samlCoreService().verifyAssertionSignature(signedAssertion(idpCredential), idp(SamlConstants.RESPONSE));

        final Assertion signedWithAnotherKey = signedAssertion(otherCredential);
        assertRejected("Signature cannot be validated",
                () -> samlCoreService().verifyAssertionSignature(signedWithAnotherKey, idp(SamlConstants.RESPONSE)));
    }

    @Test
    public void assertionSignedWithAnotherKeyIsRejected() throws Exception {

        final Assertion assertion = signedAssertion(otherCredential);

        assertRejected("Signature cannot be validated",
                () -> samlCoreService().verifyAssertionSignature(assertion, idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void sha1SignatureIsRejectedByDefault() throws Exception {

        final Assertion assertion = signedAssertion(idpCredential,
                SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, SignatureConstants.ALGO_ID_DIGEST_SHA1);

        assertRejected("signature algorithm '" + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 + "' is not allowed",
                () -> samlCoreService().verifyAssertionSignature(assertion, idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void sha1DigestIsRejectedByDefault() throws Exception {

        final Assertion assertion = signedAssertion(idpCredential,
                SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, SignatureConstants.ALGO_ID_DIGEST_SHA1);

        assertRejected("digest algorithm '" + SignatureConstants.ALGO_ID_DIGEST_SHA1 + "' is not allowed",
                () -> samlCoreService().verifyAssertionSignature(assertion, idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void sha1IsAcceptedWhenAllowed() throws Exception {

        final Assertion assertion = signedAssertion(idpCredential,
                SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, SignatureConstants.ALGO_ID_DIGEST_SHA1);

        samlCoreService().verifyAssertionSignature(assertion,
                idp(SamlConstants.ASSERTION, Collections.singletonMap(SamlConstants.ALLOW_SHA1_SIGNATURES, "true")));
    }

    @Test
    public void strongerRsaAlgorithmsAreAccepted() throws Exception {

        samlCoreService().verifyAssertionSignature(signedAssertion(idpCredential,
                SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512, SignatureConstants.ALGO_ID_DIGEST_SHA384),
                idp(SamlConstants.ASSERTION));
    }

    @Test
    public void hmacSignatureIsRejectedEvenWhenSha1IsAllowed() throws Exception {

        final Credential hmacCredential = CredentialSupport.getSimpleCredential(
                KeySupport.generateKey("HmacSHA256", 256, null));
        final Assertion assertion = signedAssertion(hmacCredential,
                SignatureConstants.ALGO_ID_MAC_HMAC_SHA256, SignatureConstants.ALGO_ID_DIGEST_SHA256);

        assertRejected("signature algorithm '" + SignatureConstants.ALGO_ID_MAC_HMAC_SHA256 + "' is not allowed",
                () -> samlCoreService().verifyAssertionSignature(assertion,
                        idp(SamlConstants.ASSERTION, Collections.singletonMap(SamlConstants.ALLOW_SHA1_SIGNATURES, "true"))));
    }

    @Test
    public void responseWithMoreThanOneAssertionIsRejected() {

        final Response response = response();
        response.getAssertions().add(assertion());
        response.getAssertions().add(assertion());

        assertRejected("exactly one unencrypted assertion",
                () -> samlCoreService().getAssertion(response, idp(SamlConstants.ASSERTION)));
    }

    @Test
    public void responseWithoutAssertionIsRejected() {

        assertRejected("exactly one unencrypted assertion",
                () -> samlCoreService().getAssertion(response(), idp(SamlConstants.ASSERTION)));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void assertRejected(final String expectedReason, final Runnable verification) {

        try {
            verification.run();
            Assert.fail("Expected a rejection: " + expectedReason);
        } catch (SamlException e) {
            Assert.assertTrue("Unexpected reason: " + e.getMessage(), e.getMessage().contains(expectedReason));
        }
    }

    private static SamlCoreServiceImpl samlCoreService() {

        final MockSamlConfigurationService configurationService = new MockSamlConfigurationService();
        return new SamlCoreServiceImpl(new CredentialServiceImpl(configurationService), null,
                metaDataService(), new MockMessageObserver(), configurationService, null);
    }

    private static Credential newCredential() throws Exception {

        final KeyPair keyPair = KeySupport.generateKeyPair("RSA", 2048, null);
        return CredentialSupport.getSimpleCredential(keyPair.getPublic(), keyPair.getPrivate());
    }

    private static Response response() {

        final Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID(newId());
        response.setIssueInstant(new DateTime());
        return response;
    }

    private static Assertion assertion() {

        final Assertion assertion = build(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(newId());
        assertion.setIssueInstant(new DateTime());
        final Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue("https://idp.example.com/metadata");
        assertion.setIssuer(issuer);
        return assertion;
    }

    private static Assertion signedAssertion(final Credential signingCredential) throws Exception {

        return signedAssertion(signingCredential, SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256,
                SignatureConstants.ALGO_ID_DIGEST_SHA256);
    }

    private static Assertion signedAssertion(final Credential signingCredential, final String signatureAlgorithm,
                                             final String digestAlgorithm) throws Exception {

        final Assertion assertion = assertion();
        final Signature signature = build(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        assertion.setSignature(signature);
        ((SAMLObjectContentReference) signature.getContentReferences().get(0)).setDigestAlgorithm(digestAlgorithm);

        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        Signer.signObject(signature);
        return assertion;
    }

    private static String newId() {

        return "_" + UUID.randomUUID().toString().replace("-", "");
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(final QName elementName) {

        return (T) builderFactory.getBuilder(elementName).buildObject(elementName);
    }

    private static IdentityProviderConfiguration idp(final String signatureValidationType) {

        return idp(signatureValidationType, Collections.emptyMap());
    }

    private static IdentityProviderConfiguration idp(final String signatureValidationType,
                                                     final Map<String, String> optionalProperties) {

        return (IdentityProviderConfiguration) Proxy.newProxyInstance(TestSamlSignatureVerification.class.getClassLoader(),
                new Class<?>[]{IdentityProviderConfiguration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":                      return "48190c8c-42c4-46af-8d1a-0cd5db894797";
                        case "getIdpName":                 return "Test IdP";
                        case "getSpEndpointHostname":      return "dotcms.example.com";
                        case "getSignatureValidationType": return signatureValidationType;
                        case "containsOptionalProperty":   return optionalProperties.containsKey((String) args[0]);
                        case "getOptionalProperty":        return optionalProperties.get((String) args[0]);
                        default:                           return null;
                    }
                });
    }

    private static MetaDataService metaDataService() {

        final MetaData metaData = new MetaData("https://idp.example.com/metadata", null, Collections.emptyMap(),
                Collections.emptyMap(), Collections.singletonList(idpCredential));
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
