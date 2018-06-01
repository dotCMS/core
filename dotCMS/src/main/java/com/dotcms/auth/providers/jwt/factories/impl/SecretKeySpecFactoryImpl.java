package com.dotcms.auth.providers.jwt.factories.impl;

import com.dotcms.auth.providers.jwt.factories.KeyFactoryUtils;
import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * @author Jonathan Gamba 5/29/18
 */
public class SecretKeySpecFactoryImpl implements SigningKeyFactory {

    public static final String DEFAULT_SECRET = "26b30b77909b6a91ec10c0692c6b82f851360b6e3dbffc685137dbf554da6d1c2540a3d4196ecdd9e9585320e6554261c99522e1e7ac824e589f8007882281bc";

    @Override
    public Key getKey() {

        //The JWT signature algorithm we will be using to sign the token
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        //Read the secret to use
        final String secret = getAndProcessSecret();

        //We will sign our JWT with our app secret key
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secret);
        return new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
    }

    /**
     * Returns a secret, the secret will be read from a property <strong>json.web.token.hash.signing.key</strong>
     * or from an file inside the assets folder, if not secret is provided we will generate one for
     * the client.
     *
     * @return The secret to use for signing the token
     */
    private String getAndProcessSecret() {

        final KeyFactoryUtils factoryUtils = KeyFactoryUtils.getInstance();

        //Read the secret hash
        String secret = Config
                .getStringProperty(
                        "json.web.token.hash.signing.key",
                        DEFAULT_SECRET);

        //The secret was not changed using the config properties file
        if (DEFAULT_SECRET.equals(secret)) {

            //Verify if the have a secret file in the assets folder
            if (factoryUtils.existSecretFile()) {

                //Read the secret file from the assets folder
                final String secretInSharedStorage = factoryUtils.readSecretFromDisk();
                //And verify is not our default secret
                if (DEFAULT_SECRET.equals(secretInSharedStorage)) {
                    //Generate a new secret
                    secret = generateSecret();
                    factoryUtils.writeSecretToDisk(secret);
                } else {
                    //Use the secret found in the assets folder
                    secret = secretInSharedStorage;
                }

            } else {//There is not secret file in the assets folder
                //Generate a new secret
                secret = generateSecret();
                factoryUtils.writeSecretToDisk(secret);
            }
        } else {
            // Use the override secret and write it to the assets folder
            factoryUtils.writeSecretToDisk(secret);
        }

        return secret;
    }

    /**
     * Generates a random secret
     */
    private String generateSecret() {

        try {
            final String randomString = new RandomString().nextString();
            return getSHA512(randomString, getSalt());
        } catch (NoSuchAlgorithmException e) {
            Logger.error(this.getClass(),
                    String.format("Unable to generate JWT secret [%s]", e.getMessage()), e);
        }

        return DEFAULT_SECRET;
    }

    /**
     * Applies a <strong>SHA-512</strong> algorithm to a given secret
     */
    private String getSHA512(String toHash, byte[] salt) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);
        byte[] bytes = md.digest(toHash.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Returns a SecureRandom byte array that implements a <strong>SHA1PRNG</strong> Random Number
     * Generator (RNG) algorithm.
     */
    private byte[] getSalt() throws NoSuchAlgorithmException {

        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    class RandomString {

        final static String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final static String digits = "0123456789";

        final Random random;
        final char[] symbols;
        final char[] buf;

        /**
         * Creates an alphanumeric strings from a secure generator
         */
        RandomString(final int length) {

            if (length < 1) {
                throw new IllegalArgumentException();
            }

            String symbolsToUse = upper + upper.toLowerCase(Locale.ROOT) + digits;

            this.random = new SecureRandom();
            this.symbols = symbolsToUse.toCharArray();
            this.buf = new char[length];
        }

        /**
         * Creates an alphanumeric strings from a secure generator of default length 100
         */
        RandomString() {
            this(100);
        }

        /**
         * Returns a random string.
         */
        String nextString() {
            for (int idx = 0; idx < buf.length; ++idx) {
                buf[idx] = symbols[random.nextInt(symbols.length)];
            }
            return new String(buf);
        }

    }

}