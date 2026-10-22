package com.dotcms.saml.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Utility class for generating self-signed certificates and key pairs for testing purposes.
 * Uses keytool to avoid reliance on internal sun.security.x509 APIs that are not accessible in Java 21+.
 */
public class MockSecurityUtils {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String ALIAS = "selfsigned";
    private static final char[] KS_PASSWORD = "changeit".toCharArray();

    /**
     * Holder for a certificate and its associated key pair, generated together via keytool.
     */
    public static class CertAndKeyPair {
        public final X509Certificate certificate;
        public final KeyPair keyPair;

        CertAndKeyPair(X509Certificate certificate, KeyPair keyPair) {
            this.certificate = certificate;
            this.keyPair = keyPair;
        }
    }

    /**
     * Generates a self-signed X.509 certificate and its associated key pair using keytool.
     *
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param days      how many days from now the Certificate is valid for
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     * @param keySize   RSA key size in bits
     * @return CertAndKeyPair containing the certificate and key pair
     */
    public static CertAndKeyPair generateCertAndKeyPair(final String dn, final int days,
                                                         final String algorithm, final int keySize) {
        try {
            final File tempFile = File.createTempFile("test-keystore-", ".p12");
            tempFile.delete(); // keytool needs to create the file itself
            tempFile.deleteOnExit();

            final String keytoolPath = System.getProperty("java.home") + File.separator
                    + "bin" + File.separator + "keytool";
            final Process process = new ProcessBuilder(
                    keytoolPath, "-genkeypair",
                    "-alias", ALIAS,
                    "-keyalg", "RSA",
                    "-keysize", String.valueOf(keySize),
                    "-validity", String.valueOf(days),
                    "-dname", dn,
                    "-sigalg", algorithm,
                    "-keystore", tempFile.getAbsolutePath(),
                    "-storetype", "PKCS12",
                    "-storepass", new String(KS_PASSWORD),
                    "-keypass", new String(KS_PASSWORD)
            ).redirectErrorStream(true).start();

            // Consume output to prevent blocking
            final String output = new String(process.getInputStream().readAllBytes());
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("keytool failed with exit code " + exitCode
                        + ", output: " + output + ", keystore path: " + tempFile.getAbsolutePath()
                        + ", keytool path: " + keytoolPath);
            }

            final KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(tempFile)) {
                ks.load(fis, KS_PASSWORD);
            }

            final X509Certificate cert = (X509Certificate) ks.getCertificate(ALIAS);
            final PrivateKey privateKey = (PrivateKey) ks.getKey(ALIAS, KS_PASSWORD);
            final PublicKey publicKey = cert.getPublicKey();
            final KeyPair keyPair = new KeyPair(publicKey, privateKey);

            return new CertAndKeyPair(cert, keyPair);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a self-signed certificate using default test parameters.
     */
    public static CertAndKeyPair generateCertAndKeyPair() {
        return generateCertAndKeyPair("CN=Test, L=London, C=GB", 365, "SHA256withRSA", 4096);
    }

    /**
     * @deprecated Use {@link #generateCertAndKeyPair()} instead which generates cert and key together.
     */
    @Deprecated
    public static KeyPair generateKeyPair() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use {@link #generateCertAndKeyPair()} instead which generates cert and key together.
     */
    @Deprecated
    public static X509Certificate generateCertificate(final String dn, final KeyPair pair,
                                                       final int days, final String algorithm) {
        return generateCertAndKeyPair(dn, days, algorithm, 4096).certificate;
    }

    /**
     * @deprecated Use {@link #generateCertAndKeyPair()} instead which generates cert and key together.
     */
    @Deprecated
    public static X509Certificate generateCertificate(final KeyPair keyPair) {
        return generateCertAndKeyPair().certificate;
    }

    public static String formatCrtFileContents(final Certificate certificate) throws CertificateEncodingException {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
        final byte[] rawCrtText = certificate.getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        return BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    }

    public static String generatePrivateKeyAsString(final KeyPair keyPair, final Certificate certificate) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final Certificate[] chain = {certificate};
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("main", keyPair.getPrivate(), "654321".toCharArray(), chain);
            keyStore.store(out, "123456".toCharArray());
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    public static String generatePrivateKeyAsString(final KeyPair keyPair) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
        final byte[] rawCrtText = keyPair.getPrivate().getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        return BEGIN_PRIVATE_KEY + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_PRIVATE_KEY;
    }
}
