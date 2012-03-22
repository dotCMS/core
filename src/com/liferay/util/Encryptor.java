/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.util.Logger;

/**
 * <a href="Encryptor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class Encryptor {

	public static final String ENCODING = "UTF8";

	public static final String DIGEST_ALGORITHM = "SHA";

	public static final String KEY_ALGORITHM = "DES";

	public static final String SUN_PROVIDER_CLASS =
		"com.sun.crypto.provider.SunJCE";

	public static final String IBM_PROVIDER_CLASS =
		"com.ibm.crypto.provider.IBMJCE";

	public static final String PROVIDER_CLASS = GetterUtil.get(
		SystemProperties.get(Encryptor.class.getName() + ".provider.class"),
		SUN_PROVIDER_CLASS);

	public static Key generateKey() throws EncryptorException {
		return generateKey(KEY_ALGORITHM);
	}

	public static Key generateKey(String algorithm) throws EncryptorException {
		try {
			Security.addProvider(getProvider());

			KeyGenerator generator = KeyGenerator.getInstance(algorithm);
			generator.init(56, new SecureRandom());

			Key key = generator.generateKey();

			return key;
		}
		catch (Exception e) {
			throw new EncryptorException(e);
		}
	}

	public static Provider getProvider()
		throws ClassNotFoundException, IllegalAccessException,
			   InstantiationException {

		Class providerClass = null;

		try {
			providerClass = Class.forName(PROVIDER_CLASS);
		}
		catch (ClassNotFoundException cnfe) {
			if ((ServerDetector.isWebSphere()) &&
				(PROVIDER_CLASS.equals(SUN_PROVIDER_CLASS))) {

				_log.warn(
					"WebSphere does not have " + SUN_PROVIDER_CLASS +
						", using " + IBM_PROVIDER_CLASS + " instead");

				providerClass = Class.forName(IBM_PROVIDER_CLASS);
			}
			else {
				throw cnfe;
			}
		}

		return (Provider)providerClass.newInstance();
	}

	public static String decrypt(Key key, String encryptedString)
		throws EncryptorException {

		try {
			Security.addProvider(getProvider());

			Cipher cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, key);

			byte[] encryptedBytes = Base64.decode(encryptedString);
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

			String decryptedString = new String(decryptedBytes, ENCODING);

			return decryptedString;
		}
		catch (Exception e) {
			throw new EncryptorException(e);
		}
	}

	public static String digest(String text) {
		return digest(DIGEST_ALGORITHM, text);
	}

	public static String digest(String algorithm, String text) {
		MessageDigest mDigest = null;

		try{
			mDigest = MessageDigest.getInstance(algorithm);

			mDigest.update(text.getBytes(ENCODING));
		}
		catch(NoSuchAlgorithmException nsae) {
			Logger.error(Encryptor.class,nsae.getMessage(),nsae);
			
		}
		catch(UnsupportedEncodingException uee) {
			Logger.error(Encryptor.class,uee.getMessage(),uee);
		}

		byte raw[] = mDigest.digest();

		return Base64.encode(raw);
	}

	public static String encrypt(Key key, String plainText)
		throws EncryptorException {

		try {
			Security.addProvider(getProvider());

			Cipher cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, key);

			byte[] decryptedBytes = plainText.getBytes(ENCODING);
			byte[] encryptedBytes = cipher.doFinal(decryptedBytes);

			String encryptedString = Base64.encode(encryptedBytes);

			return encryptedString;
		}
		catch (Exception e) {
			throw new EncryptorException(e);
		}
	}

	private static final Log _log = LogFactory.getLog(Encryptor.class);

}