package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.cert.Certificate;

public class MockIdentityProviderConfigurationFactory implements IdentityProviderConfigurationFactory {

    private static final MockSecurityUtils.CertAndKeyPair CERT_AND_KEY = MockSecurityUtils.generateCertAndKeyPair();
    private static final KeyPair KEY_PAIR        = CERT_AND_KEY.keyPair;
    private static final Certificate CERTIFICATE = CERT_AND_KEY.certificate;

    @Override
    public IdentityProviderConfiguration findIdentityProviderConfigurationById(String s) {
        return new IdentityProviderConfiguration() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public String getSpIssuerURL() {
                return "https://test.com";
            }

            @Override
            public String getIdpName() {
                return "test.com/sp";
            }

            @Override
            public String getId() {
                return "123";
            }

            @Override
            public String getSpEndpointHostname() {
                return "test.com";
            }

            @Override
            public String getSignatureValidationType() {
                return "signature";
            }

            @Override
            public char[] getIdPMetadataFile() {

                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (InputStream in = MockIdentityProviderConfigurationFactory.class.getClassLoader()
                        .getResourceAsStream("./TestIDPMetadata-dotcms.com.xml")) {

                    IOUtils.copy(in, outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return outputStream.toString().toCharArray();
            }

            @Override
            public char[] getPublicCert() {
                try {

                    return MockSecurityUtils.formatCrtFileContents(CERTIFICATE).toCharArray();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public char[] getPrivateKey() {
                return MockSecurityUtils.generatePrivateKeyAsString(KEY_PAIR).toCharArray();
            }

            @Override
            public Object getOptionalProperty(String s) {
                return null;
            }

            @Override
            public boolean containsOptionalProperty(String s) {
                return false;
            }

            @Override
            public void destroy() {

            }
        };
    }
}
