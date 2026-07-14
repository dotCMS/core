package com.dotcms.auth.dotAuth.rest.handler;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

/**
 * Generates a self-signed RSA keypair for SAML SP request signing.
 * Called by {@link SamlProtocolHandler} when an admin saves a SAML
 * config without providing their own key/cert.
 */
final class SamlKeyPairGenerator {

    private static final int KEY_SIZE = 2048;
    private static final long VALIDITY_YEARS = 10;
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SamlKeyPairGenerator() {}

    static GeneratedKeyPair generate(final String hostname) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE, SECURE_RANDOM);
            final KeyPair keyPair = keyGen.generateKeyPair();

            final String cn = UtilMethods.isSet(hostname)
                    ? hostname
                    : "dotCMS SAML SP";
            final X500Name subject = new X500Name("CN=" + cn);

            final Instant now = Instant.now();
            final Date notBefore = Date.from(now);
            final Date notAfter  = Date.from(now.plus(VALIDITY_YEARS * 365, ChronoUnit.DAYS));

            final X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(now.toEpochMilli()),
                    notBefore,
                    notAfter,
                    subject,
                    keyPair.getPublic());

            final ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .build(keyPair.getPrivate());
            final X509CertificateHolder holder = certBuilder.build(signer);
            final X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

            return new GeneratedKeyPair(toPem(keyPair.getPrivate()), toPem(cert));
        } catch (Exception e) {
            Logger.error(SamlKeyPairGenerator.class,
                    "Failed to generate SAML SP keypair: " + e.getMessage(), e);
            throw new IllegalStateException("SAML SP keypair generation failed", e);
        }
    }

    private static String toPem(final Object obj) throws Exception {
        final StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            if (obj instanceof PrivateKey) {
                // Force the PKCS#8 "BEGIN PRIVATE KEY" label. JcaPEMWriter re-labels
                // RSA keys as PKCS#1 ("BEGIN RSA PRIVATE KEY") even when handed a
                // PrivateKeyInfo (observed on BC 1.84), and the SAML bundle's
                // IdpConfigCredentialResolver only strips the PKCS#8 header before
                // base64-decoding — a PKCS#1 header corrupts the decode.
                pw.writeObject(new PemObject("PRIVATE KEY", ((PrivateKey) obj).getEncoded()));
            } else {
                pw.writeObject(obj);
            }
        }
        return sw.toString().trim();
    }

    static final class GeneratedKeyPair {
        final String privateKeyPem;
        final String publicCertPem;

        GeneratedKeyPair(final String privateKeyPem, final String publicCertPem) {
            this.privateKeyPem = privateKeyPem;
            this.publicCertPem = publicCertPem;
        }
    }
}
