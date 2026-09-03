package com.dotcms.auth.dotAuth.rest.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

public class SamlKeyPairGeneratorTest {

    /**
     * The SAML bundle's IdpConfigCredentialResolver strips exactly the PKCS#8
     * "BEGIN/END PRIVATE KEY" markers and base64-decodes the rest. A PKCS#1
     * ("BEGIN RSA PRIVATE KEY") header survives the strip and corrupts the
     * decode ("Invalid lenByte"), so the generated PEM must be PKCS#8-labeled
     * and must decode through the resolver's exact logic.
     */
    @Test
    public void generated_private_key_decodes_via_saml_bundle_logic() throws Exception {
        final SamlKeyPairGenerator.GeneratedKeyPair kp =
                SamlKeyPairGenerator.generate("sp.example.com");

        assertTrue("private key must use the PKCS#8 PEM label",
                kp.privateKeyPem.startsWith("-----BEGIN PRIVATE KEY-----\n"));

        // Verbatim replication of IdpConfigCredentialResolver.getPrivateKey
        String key = kp.privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----\n", "")
                .replace("-----END PRIVATE KEY-----", "");
        final PrivateKey decoded = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.decodeBase64(key)));
        assertEquals("RSA", decoded.getAlgorithm());
    }

    @Test
    public void generated_cert_is_pem_encoded() {
        final SamlKeyPairGenerator.GeneratedKeyPair kp =
                SamlKeyPairGenerator.generate("sp.example.com");
        assertTrue(kp.publicCertPem.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(kp.publicCertPem.endsWith("-----END CERTIFICATE-----"));
    }
}
