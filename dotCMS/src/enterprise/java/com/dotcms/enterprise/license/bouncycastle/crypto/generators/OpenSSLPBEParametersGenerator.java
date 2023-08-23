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

package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.PBEParametersGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.MD5Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Generator for PBE derived keys and ivs as usd by OpenSSL.
 * <p>
 * The scheme is a simple extension of PKCS 5 V2.0 Scheme 1 using MD5 with an
 * iteration count of 1.
 * <p>
 */
public class OpenSSLPBEParametersGenerator
    extends PBEParametersGenerator
{
    private Digest  digest = new MD5Digest();

    /**
     * Construct a OpenSSL Parameters generator. 
     */
    public OpenSSLPBEParametersGenerator()
    {
    }

    /**
     * Initialise - note the iteration count for this algorithm is fixed at 1.
     * 
     * @param password password to use.
     * @param salt salt to use.
     */
    public void init(
       byte[] password,
       byte[] salt)
    {
        super.init(password, salt, 1);
    }
    
    /**
     * the derived key function, the ith hash of the password and the salt.
     */
    private byte[] generateDerivedKey(
        int bytesNeeded)
    {
        byte[]  buf = new byte[digest.getDigestSize()];
        byte[]  key = new byte[bytesNeeded];
        int     offset = 0;
        
        for (;;)
        {
            digest.update(password, 0, password.length);
            digest.update(salt, 0, salt.length);

            digest.doFinal(buf, 0);
            
            int len = (bytesNeeded > buf.length) ? buf.length : bytesNeeded;
            System.arraycopy(buf, 0, key, offset, len);
            offset += len;

            // check if we need any more
            bytesNeeded -= len;
            if (bytesNeeded == 0)
            {
                break;
            }

            // do another round
            digest.reset();
            digest.update(buf, 0, buf.length);
        }
        
        return key;
    }

    /**
     * Generate a key parameter derived from the password, salt, and iteration
     * count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     * @exception IllegalArgumentException if the key length larger than the base hash size.
     */
    public CipherParameters generateDerivedParameters(
        int keySize)
    {
        keySize = keySize / 8;

        byte[]  dKey = generateDerivedKey(keySize);

        return new KeyParameter(dKey, 0, keySize);
    }

    /**
     * Generate a key with initialisation vector parameter derived from
     * the password, salt, and iteration count we are currently initialised
     * with.
     *
     * @param keySize the size of the key we want (in bits)
     * @param ivSize the size of the iv we want (in bits)
     * @return a ParametersWithIV object.
     * @exception IllegalArgumentException if keySize + ivSize is larger than the base hash size.
     */
    public CipherParameters generateDerivedParameters(
        int     keySize,
        int     ivSize)
    {
        keySize = keySize / 8;
        ivSize = ivSize / 8;

        byte[]  dKey = generateDerivedKey(keySize + ivSize);

        return new ParametersWithIV(new KeyParameter(dKey, 0, keySize), dKey, keySize, ivSize);
    }

    /**
     * Generate a key parameter for use with a MAC derived from the password,
     * salt, and iteration count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     * @exception IllegalArgumentException if the key length larger than the base hash size.
     */
    public CipherParameters generateDerivedMacParameters(
        int keySize)
    {
        return generateDerivedParameters(keySize);
    }
}
