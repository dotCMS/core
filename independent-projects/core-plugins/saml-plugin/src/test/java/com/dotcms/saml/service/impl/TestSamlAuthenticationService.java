package com.dotcms.saml.service.impl;

import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlServiceBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectChildrenList;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameID;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestSamlAuthenticationService {

    @Test
    public void testIsValidSamlRequest_TRUE () {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());
        final Map<String, String>  requestParamMap = new HashMap<>();
        final MockRequest  mockRequest             = new MockRequest();
        requestParamMap.put("SAMLResponse", "xxxxxxxx");
        mockRequest.addParameters(requestParamMap);

        Assert.assertTrue(samlAuthenticationService.isValidSamlRequest(mockRequest, null, idpFactory.findIdentityProviderConfigurationById("test.dotcms.com")));
    }

    @Test
    public void testIsValidSamlRequest_FALSE () {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());
        final MockRequest  mockRequest             = new MockRequest();

        Assert.assertFalse(samlAuthenticationService.isValidSamlRequest(mockRequest, null, idpFactory.findIdentityProviderConfigurationById("test.dotcms.com")));
    }

    @Test
    public void testRenderMetadataXML () {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());
        final StringWriter stringWriter            = new StringWriter();

        samlAuthenticationService.renderMetadataXML(stringWriter, idpFactory.findIdentityProviderConfigurationById("test.dotcms.com"));

        Assert.assertTrue(stringWriter.toString().length() > 0);
        Assert.assertTrue(stringWriter.toString().contains("EntityDescriptor"));
        Assert.assertTrue(stringWriter.toString().contains("entityID=\"https://test.com\""));
        Assert.assertTrue(stringWriter.toString().contains("X509Certificate"));
        Assert.assertTrue(stringWriter.toString().contains("EncryptionMethod"));
    }

    @Test
    public void testGetValue_notNull_OK () {

        final String namespaceURI     = "uri:";
        final String elementLocalName = "nameID";
        final String namespacePrefix  = "prefix:";
        final String value            = "123";
        final NameID nameID = new MockNameID(namespaceURI, elementLocalName, namespacePrefix);
        nameID.setValue(value);

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final String nameIDValue = samlAuthenticationService.getValue(nameID);
        Assert.assertNotNull(nameIDValue);
        Assert.assertEquals(value, nameIDValue);
    }

    @Test
    public void testGetValue_Null_FALSE () {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final String nameIDValue = samlAuthenticationService.getValue(null);
        Assert.assertNull(nameIDValue);
    }

    @Test
    public void testGetValues_notNull_OK () {

        final String namespaceURI     = "uri:";
        final String elementLocalName = "attribute";
        final String namespacePrefix  = "prefix:";
        final String value            = "123";
        final NameID nameID = new MockNameID(namespaceURI, elementLocalName, namespacePrefix);
        nameID.setValue(value);
        final Collection<XMLObject> newElements = Arrays.asList(nameID);
        final XMLObjectChildrenList<XMLObject> attributeValues = new XMLObjectChildrenList<>(
                new MockActionNamespace(namespaceURI, "root", namespacePrefix), newElements);
        final Attribute attribute = new MockAttribute(namespaceURI, elementLocalName, namespacePrefix, attributeValues);

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final List<String> nameIDValues = samlAuthenticationService.getValues(attribute);
        Assert.assertNotNull(nameIDValues);
        Assert.assertTrue(nameIDValues.size() > 0);
        Assert.assertEquals(1, nameIDValues.size());
        Assert.assertEquals(value, nameIDValues.get(0));
    }

    @Test
    public void testGetValues_Null_FALSE () {

        final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();
        final SamlServiceBuilder samlServiceBuilder = new SamlServiceBuilderImpl();
        final SamlAuthenticationService samlAuthenticationService = samlServiceBuilder.buildAuthenticationService(
                idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

        final List<String> nameIDValues = samlAuthenticationService.getValues(null);
        Assert.assertNull(nameIDValues);
    }

}
