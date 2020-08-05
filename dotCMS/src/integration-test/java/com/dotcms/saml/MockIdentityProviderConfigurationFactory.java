package com.dotcms.saml;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.cert.Certificate;

public class MockIdentityProviderConfigurationFactory implements IdentityProviderConfigurationFactory {


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

                return new char[0];
            }

            @Override
            public char[] getPublicCert() {
                return new char[0];
            }

            @Override
            public char[] getPrivateKey() {
                return new char[0];
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
