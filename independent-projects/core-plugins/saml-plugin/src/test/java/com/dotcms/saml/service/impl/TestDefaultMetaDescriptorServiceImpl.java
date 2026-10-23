package com.dotcms.saml.service.impl;

import com.dotcms.saml.service.external.MetaData;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import com.dotcms.saml.utils.InstanceUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


public class TestDefaultMetaDescriptorServiceImpl {

    @Test
    public void testParse () throws IOException {

        try (InputStream in = TestDefaultMetaDescriptorServiceImpl.class.getClassLoader()
                .getResourceAsStream("./TestIDPMetadata-dotcms.com.xml")) {

            final MockIdentityProviderConfigurationFactory idpFactory = new MockIdentityProviderConfigurationFactory();

            new SamlServiceBuilderImpl().buildAuthenticationService(idpFactory, null, new MockMessageObserver(), new MockSamlConfigurationService());

            final MetaDescriptorService metaDescriptorService = InstanceUtil.getInstance(MetaDescriptorService.class);

            final MetaData metaData = metaDescriptorService.parse(in, idpFactory.findIdentityProviderConfigurationById("test"));

            Assert.assertEquals("https://test.dotcms.com/o/saml2?idpid=xxxx", metaData.getEntityId());
            Assert.assertFalse(metaData.getCredentialSigningList().isEmpty());

            final Map<String, String> singleSignOnBindingLocationMap = metaData.getSingleSignOnBindingLocationMap();
            final Map<String, String> singleLogoutBindingLocationMap = metaData.getSingleLogoutBindingLocationMap();

            Assert.assertFalse(singleSignOnBindingLocationMap.isEmpty());
            Assert.assertFalse(singleLogoutBindingLocationMap.isEmpty());

            Assert.assertEquals("https://test.dotcms.com/o/saml2/idp?idpid=xxx", singleSignOnBindingLocationMap.get("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"));
            Assert.assertEquals("https://test.dotcms.com/o/saml2/idp?idpid=xxx", singleSignOnBindingLocationMap.get("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"));

            Assert.assertEquals("https://test.dotcms.com/o/saml2/idp/logout?idpid=xxx", singleLogoutBindingLocationMap.get("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"));
            Assert.assertEquals("https://test.dotcms.com/o/saml2/idp/logout?idpid=xxx", singleLogoutBindingLocationMap.get("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"));

        }
    }
}
