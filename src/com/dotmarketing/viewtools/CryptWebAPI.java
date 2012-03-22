package com.dotmarketing.viewtools;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;

/**
 * Encrypt and decrypt text using PublicEncryptionFactory methods.
 *
 * @author  Armando Siem
 * @since   1.5.0
 * @version 1.0.0
 * @see     PublicEncryptionFactory
 */

public class CryptWebAPI {
	/**
	  * Encrypt a text
	  * @param	text parameter with the text to be encrypted
	  * @return	String with the encrypted text
	  * @see	java.lang.String
	  */
	public static String crypt(String text) {
		try {
			return PublicEncryptionFactory.encryptString(text);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	  * Decrypt a text
	  * @param	text parameter with the text to be decrypted
	  * @return	String with the decrypted text
	  * @see	java.lang.String
	  */
	public static String decrypt(String text) {
		try {
			return PublicEncryptionFactory.decryptString(text);
		} catch (Exception e) {
			return null;
		}
	}
}