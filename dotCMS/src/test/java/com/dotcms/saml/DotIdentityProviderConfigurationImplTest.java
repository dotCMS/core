package com.dotcms.saml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DotIdentityProviderConfigurationImplTest {

    @Test
    public void supported_signature_validation_types_are_kept() {
        assertEquals(DotSamlConstants.RESPONSE,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType("response"));
        assertEquals(DotSamlConstants.ASSERTION,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType(" Assertion "));
        assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType("responseandassertion"));
    }

    @Test
    public void unsupported_signature_validation_types_require_both_signatures() {
        for (final String configured : new String[]{"none", "NONE", "", "  ", null, "signature"}) {
            assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION,
                    DotIdentityProviderConfigurationImpl.resolveSignatureValidationType(configured),
                    "type '" + configured + "'");
        }
    }
}
