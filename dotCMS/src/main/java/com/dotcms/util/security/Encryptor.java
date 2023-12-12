package com.dotcms.util.security;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.liferay.util.EncryptorException;

import java.io.Serializable;
import java.security.Key;
import java.security.MessageDigest;

/**
 * This class is just a wrapper to encapsulate the
 * {@link com.liferay.util.Encryptor} This approach provides the ability to
 * inject, proxy, mock, use diff implementation based on a contract etc.
 *
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public interface Encryptor extends Serializable {

	/**
	 * Generates an encryption key based on our default key algorithm.
	 * 
	 * @return The encryption key.
	 * @throws EncryptorException
	 *             The Key Generator could not be accessed.
	 */
    default Key generateKey() throws EncryptorException {

        return com.liferay.util.Encryptor.generateKey();
    }

	/**
	 * Generates an encryption key based on the specified algorithm.
	 * 
	 * @param algorithm
	 *            - The encryption algorithm for the key.
	 * @return The encryption key.
	 * @throws EncryptorException
	 *             The Key Generator could not be accessed.
	 */
    default Key generateKey(final String algorithm) throws EncryptorException {

        return com.liferay.util.Encryptor.generateKey(algorithm);
    }


	/**
	 * Decrypts an encrypted String using the specified key.
	 * 
	 * @param key
	 *            - The encryption key.
	 * @param encryptedString
	 *            - The String that will be decrypted.
	 * @return The decrypted String.
	 * @throws EncryptorException
	 *             An error occurred during the decrypting process.
	 */
    default String decrypt(final Key key,
                                  final String encryptedString)
            throws EncryptorException {

        return com.liferay.util.Encryptor.decrypt(key, encryptedString);
    }

	/**
	 * Allows to verify if a plain text String is unmodified based on the
	 * default digest algorithm.
	 * 
	 * @param text
	 *            - The plain text.
	 * @return A hash code representing the plain text.
	 */
    default String digest(final String text) {

        return com.liferay.util.Encryptor.digest(text);
    }

	/**
	 * Allows to verify if a plain text String is unmodified based on the
	 * specified digest algorithm.
	 * 
	 * @param algorithm
	 *            - The digest algorithm.
	 * @param text
	 *            - The plain text.
	 * @return A hash code representing the plain text.
	 */
    default String digest(final String algorithm,
                                 final String text) {

       return com.liferay.util.Encryptor.digest(algorithm, text);
    }

	/**
	 * Encrypts a String using the specified key.
	 * 
	 * @param key
	 *            - The encryption key.
	 * @param plainText
	 *            - The String to encrypt.
	 * @return The encrypted String.
	 * @throws EncryptorException
	 *             An error occurred during the encrypting process.
	 */
    default String encrypt(final Key key, final String plainText)
            throws EncryptorException {

        return com.liferay.util.Encryptor.encrypt(key, plainText);
    }

	/**
	 * Encrypts the specified String using Liferay's default company key.
	 * 
	 * @param plainText
	 *            - The String to encrypt.
	 * @return The encrypted String.
	 */
    default String encryptString(final String plainText) {

        return PublicEncryptionFactory.encryptString(plainText);
    }

	/**
	 * Decrypts the specified String using Liferay's default company key.
	 * 
	 * @param plainText
	 *            - The String to decrypt.
	 * @return The decrypted String.
	 */
    default String decryptString(final String plainText) {

        return PublicEncryptionFactory.decryptString(plainText);
    }

	/**
	 * This method performs a relatively simple implementation of hashing a String value using the
	 * specified {@link MessageDigest} instance. It's worth noting that the resulting String does
	 * not include slash characters at all.
	 *
	 * @param value    The value being encrypted.
	 * @param digester The {@link MessageDigest} instance that will process the encryption.
	 *
	 * @return The encrypted value.
	 */
	default String encryptString(final String value, final MessageDigest digester) {
		final byte[] hash = digester.digest(value.getBytes());
		final StringBuilder hexString = new StringBuilder();
		for (final byte b : hash) {
			hexString.append(String.format("%02x", b));
		}
		return hexString.toString();
	}

} // E:O:F:Encryptor.
