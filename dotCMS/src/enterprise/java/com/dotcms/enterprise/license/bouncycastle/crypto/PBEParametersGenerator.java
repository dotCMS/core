/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.crypto;

import com.dotcms.enterprise.license.bouncycastle.util.Strings;

/**
 * super class for all Password Based Encryption (PBE) parameter generator classes.
 */
public abstract class PBEParametersGenerator
{
    protected byte[]  password;
    protected byte[]  salt;
    protected int     iterationCount;

    /**
     * base constructor.
     */
    protected PBEParametersGenerator()
    {
    }

    /**
     * initialise the PBE generator.
     *
     * @param password the password converted into bytes (see below).
     * @param salt the salt to be mixed with the password.
     * @param iterationCount the number of iterations the "mixing" function
     * is to be applied for.
     */
    public void init(
        byte[]  password,
        byte[]  salt,
        int     iterationCount)
    {
        this.password = password;
        this.salt = salt;
        this.iterationCount = iterationCount;
    }

    /**
     * return the password byte array.
     *
     * @return the password byte array.
     */
    public byte[] getPassword()
    {
        return password;
    }

    /**
     * return the salt byte array.
     *
     * @return the salt byte array.
     */
    public byte[] getSalt()
    {
        return salt;
    }

    /**
     * return the iteration count.
     *
     * @return the iteration count.
     */
    public int getIterationCount()
    {
        return iterationCount;
    }

    /**
     * generate derived parameters for a key of length keySize.
     *
     * @param keySize the length, in bits, of the key required.
     * @return a parameters object representing a key.
     */
    public abstract CipherParameters generateDerivedParameters(int keySize);

    /**
     * generate derived parameters for a key of length keySize, and
     * an initialisation vector (IV) of length ivSize.
     *
     * @param keySize the length, in bits, of the key required.
     * @param ivSize the length, in bits, of the iv required.
     * @return a parameters object representing a key and an IV.
     */
    public abstract CipherParameters generateDerivedParameters(int keySize, int ivSize);

    /**
     * generate derived parameters for a key of length keySize, specifically
     * for use with a MAC.
     *
     * @param keySize the length, in bits, of the key required.
     * @return a parameters object representing a key.
     */
    public abstract CipherParameters generateDerivedMacParameters(int keySize);

    /**
     * converts a password to a byte array according to the scheme in
     * PKCS5 (ascii, no padding)
     *
     * @param password a character array reqpresenting the password.
     * @return a byte array representing the password.
     */
    public static byte[] PKCS5PasswordToBytes(
        char[]  password)
    {
        byte[]  bytes = new byte[password.length];

        for (int i = 0; i != bytes.length; i++)
        {
            bytes[i] = (byte)password[i];
        }

        return bytes;
    }

    /**
     * converts a password to a byte array according to the scheme in
     * PKCS5 (UTF-8, no padding)
     *
     * @param password a character array reqpresenting the password.
     * @return a byte array representing the password.
     */
    public static byte[] PKCS5PasswordToUTF8Bytes(
        char[]  password)
    {
        return Strings.toUTF8ByteArray(password);
    }

    /**
     * converts a password to a byte array according to the scheme in
     * PKCS12 (unicode, big endian, 2 zero pad bytes at the end).
     *
     * @param password a character array representing the password.
     * @return a byte array representing the password.
     */
    public static byte[] PKCS12PasswordToBytes(
        char[]  password)
    {
        if (password.length > 0)
        {
                                       // +1 for extra 2 pad bytes.
            byte[]  bytes = new byte[(password.length + 1) * 2];

            for (int i = 0; i != password.length; i ++)
            {
                bytes[i * 2] = (byte)(password[i] >>> 8);
                bytes[i * 2 + 1] = (byte)password[i];
            }

            return bytes;
        }
        else
        {
            return new byte[0];
        }
    }
}
