package com.dotcms.security.apps;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.apache.commons.lang3.ArrayUtils;

public class AppsUtil {

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    /**
     * Given the AppSecrets this will return a char array representing the deserialized json object.
     * No strings are created in the transformation process.
     * @param object
     * @return
     * @throws DotDataException
     */
    static char[] toJsonAsChars(final AppSecrets object) throws DotDataException {
        try {
            final byte [] bytes = mapper.writeValueAsBytes(object);
            return bytesToCharArrayUTF(bytes);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Takes a char array representation of a json secret and generates the domain object.
     * No strings are created in the transformation process.
     * @param chars
     * @return The Secrets domain model.
     * @throws DotDataException
     */
    static AppSecrets readJson(final char[] chars) throws DotDataException {
        try {
            final byte [] bytes = charsToBytesUTF(chars);
            return mapper.readValue(bytes, AppSecrets.class);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * This method takes a byte array and converts its contents into a char array
     * No String middle man is created.
     * https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
     * @param bytes
     * @return char array
     */
    static char[] bytesToCharArrayUTF(final byte[] bytes) throws IOException {
        final List<Integer> integers = new ArrayList<>(bytes.length);
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {

            int chr;
            while ((chr = reader.read()) != -1) {
                integers.add(chr);
            }
        }
        return ArrayUtils.toPrimitive(
                integers.stream().map(value -> (char) value.intValue()).toArray(Character[]::new));
    }

    /**
     * This method takes a char array and converts its contents into a byte array No String middle
     * man is created. https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
     * @param chars input
     * @return byte array
     */
    static byte[] charsToBytesUTF(final char[] chars) throws IOException {
        final CharSequence sequence = java.nio.CharBuffer.wrap(chars);
        return ByteStreams
                .toByteArray(new CharSequenceInputStream(sequence, StandardCharsets.UTF_8));
    }


    /**
     * Encrypt variant of the function of the same name located in The Encryptor util class.
     * The main difference is that this ones does not use a string in the middle. It directly takes a char array.
     * And doesn't use a string internally.
     * @see Encryptor#encrypt(Key, String)
     * @param key security Key
     * @param chars
     * @return encrypted text as a char array
     * @throws EncryptorException
     */
    static char[] encrypt(final Key key, final char[] chars)
            throws EncryptorException {

        try {
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            final byte[] decryptedBytes = charsToBytesUTF(chars);
            final byte[] encryptedBytes = cipher.doFinal(decryptedBytes);
            final String encryptedString = Base64.encode(encryptedBytes);
            return encryptedString.toCharArray();
        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * Decrypt variant of the function of the same name located in The Encryptor util class.
     * The main difference is that this ones does not use a string in the middle to extract the resulting bytes.
     * @see Encryptor#decrypt(Key, String)
     * @param key security Key
     * @param encryptedString
     * @return decrypted text as a char array
     * @throws EncryptorException
     */
    static char[] decrypt(final Key key, final String encryptedString)
            throws EncryptorException {

        try {
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            final byte[] encryptedBytes = Base64.decode(encryptedString);
            final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return bytesToCharArrayUTF(decryptedBytes);

        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * This method serves as a bridge to Encryptor{@link #digest(String)}
     * @param text
     * @return
     */
    public static String digest(final String text) {
        return Encryptor.digest(text);
    }




}
