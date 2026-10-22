package com.dotcms.saml.service.impl;

import com.dotcms.saml.SamlServiceBuilder;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.InstanceUtil;
import org.junit.Assert;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.metadata.Endpoint;

public class TestSamlCoreService {

    @Test
    public void testBuildSAMLObject() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();

        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final NameID nameID = samlCoreService.buildSAMLObject(NameID.class);
        Assert.assertNotNull(nameID);
    }

    @Test
    public void testBuildLogoutRequest() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final NameID nameID = samlCoreService.buildSAMLObject(NameID.class);
        final String sessionIndexValue = "123";
        nameID.setValue(sessionIndexValue);
        final LogoutRequest logoutRequest = samlCoreService.buildLogoutRequest(
                idpFactory.findIdentityProviderConfigurationById("test.com"), nameID, sessionIndexValue);
        Assert.assertNotNull(logoutRequest);
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:logout:user", logoutRequest.getReason());
        Assert.assertEquals(sessionIndexValue, logoutRequest.getNameID().getValue());
        Assert.assertFalse(logoutRequest.getSessionIndexes().isEmpty());
        Assert.assertEquals(sessionIndexValue, logoutRequest.getSessionIndexes().get(0).getSessionIndex());
        Assert.assertEquals("https://test.dotcms.com/o/saml2/idp/logout?idpid=xxx", logoutRequest.getDestination());
    }

    @Test
    public void testBuildAuthnRequest() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final AuthnRequest authnRequest = samlCoreService.buildAuthnRequest(mockRequest,
                idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(authnRequest);
        Assert.assertEquals("https://test.com/dotsaml/login/123", authnRequest.getAssertionConsumerServiceURL());
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:protocol", authnRequest.getNameIDPolicy().getElementQName().getNamespaceURI());
        Assert.assertEquals("https://test.dotcms.com/o/saml2/idp?idpid=xxx", authnRequest.getDestination());
    }

    @Test
    public void testGetIPDSSODestination() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final String destination = samlCoreService.getIPDSLODestination(
                idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(destination);
        Assert.assertEquals("https://test.dotcms.com/o/saml2/idp/logout?idpid=xxx", destination);
    }

    @Test
    public void testGetAssertionConsumerEndpoint() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final String consumerEndpoint = samlCoreService.getAssertionConsumerEndpoint(mockRequest,
                idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(consumerEndpoint);
        Assert.assertEquals("https://test.com/dotsaml/login/123", consumerEndpoint);
    }

    @Test
    public void testBuildIssuer() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final Issuer issuer = samlCoreService.buildIssuer(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(issuer);
        Assert.assertEquals("https://test.com", issuer.getValue());
    }

    @Test
    public void testGetSPIssuerValue() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final String spIssuerValue = samlCoreService.getSPIssuerValue(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(spIssuerValue);
        Assert.assertEquals("https://test.com", spIssuerValue);
    }

    @Test
    public void testBuildNameIdPolicy() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final NameIDPolicy nameIDPolicy = samlCoreService.buildNameIdPolicy(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(nameIDPolicy);
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:protocol", nameIDPolicy.getElementQName().getNamespaceURI());
    }

    @Test
    public void testBuildRequestedAuthnContext() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final RequestedAuthnContext requestedAuthnContext = samlCoreService.buildRequestedAuthnContext(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(requestedAuthnContext);
        Assert.assertEquals("minimum", requestedAuthnContext.getComparison().toString());
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:protocol", requestedAuthnContext.getElementQName().getNamespaceURI());
    }

    @Test
    public void testGetAuthnContextComparisonTypeEnumeration() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final AuthnContextComparisonTypeEnumeration authnContextComparisonTypeEnumeration =
                samlCoreService.getAuthnContextComparisonTypeEnumeration(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(authnContextComparisonTypeEnumeration);
        Assert.assertEquals("minimum", authnContextComparisonTypeEnumeration.toString());
    }

    @Test
    public void testGetIdentityProviderSLODestinationEndpoint() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final Endpoint endpoint =
                samlCoreService.getIdentityProviderSLODestinationEndpoint(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(endpoint);
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect", endpoint.getBinding());
        Assert.assertEquals("https://test.dotcms.com/o/saml2/idp/logout?idpid=xxx", endpoint.getLocation());
    }

    @Test
    public void testGetIdentityProviderDestinationEndpoint() {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final SamlCoreService samlCoreService = InstanceUtil.getInstance(SamlCoreService.class);
        final MockRequest     mockRequest     = new MockRequest();

        mockRequest.setRequestURI("/dotAdmin");

        final Endpoint endpoint =
                samlCoreService.getIdentityProviderDestinationEndpoint(idpFactory.findIdentityProviderConfigurationById("test.com"));
        Assert.assertNotNull(endpoint);
        Assert.assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect", endpoint.getBinding());
        Assert.assertEquals("https://test.dotcms.com/o/saml2/idp?idpid=xxx", endpoint.getLocation());
    }



}
