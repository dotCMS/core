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
import com.dotcms.enterprise.license.bouncycastle.crypto.ExtendedDigest;
import com.dotcms.enterprise.license.bouncycastle.crypto.PBEParametersGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Generator for PBE derived keys and ivs as defined by PKCS 12 V1.0.
 * <p>
 * The document this implementation is based on can be found at
 * <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-12/index.html>
 * RSA's PKCS12 Page</a>
 */
public class PKCS12ParametersGenerator
    extends PBEParametersGenerator
{
    public static final int KEY_MATERIAL = 1;
    public static final int IV_MATERIAL  = 2;
    public static final int MAC_MATERIAL = 3;

    private Digest digest;

    private int     u;
    private int     v;

    /**
     * Construct a PKCS 12 Parameters generator. This constructor will
     * accept any digest which also implements ExtendedDigest.
     *
     * @param digest the digest to be used as the source of derived keys.
     * @exception IllegalArgumentException if an unknown digest is passed in.
     */
    public PKCS12ParametersGenerator(
        Digest  digest)
    {
        this.digest = digest;
        if (digest instanceof ExtendedDigest)
        {
            u = digest.getDigestSize();
            v = ((ExtendedDigest)digest).getByteLength();
        }
        else
        {
            throw new IllegalArgumentException("Digest " + digest.getAlgorithmName() + " unsupported");
        }
    }

    /**
     * add a + b + 1, returning the result in a. The a value is treated
     * as a BigInteger of length (b.length * 8) bits. The result is 
     * modulo 2^b.length in case of overflow.
     */
    private void adjust(
        byte[]  a,
        int     aOff,
        byte[]  b)
    {
        int  x = (b[b.length - 1] & 0xff) + (a[aOff + b.length - 1] & 0xff) + 1;

        a[aOff + b.length - 1] = (byte)x;
        x >>>= 8;

        for (int i = b.length - 2; i >= 0; i--)
        {
            x += (b[i] & 0xff) + (a[aOff + i] & 0xff);
            a[aOff + i] = (byte)x;
            x >>>= 8;
        }
    }

    /**
     * generation of a derived key ala PKCS12 V1.0.
     */
    private byte[] generateDerivedKey(
        int idByte,
        int n)
    {
        byte[]  D = new byte[v];
        byte[]  dKey = new byte[n];

        for (int i = 0; i != D.length; i++)
        {
            D[i] = (byte)idByte;
        }

        byte[]  S;

        if ((salt != null) && (salt.length != 0))
        {
            S = new byte[v * ((salt.length + v - 1) / v)];

            for (int i = 0; i != S.length; i++)
            {
                S[i] = salt[i % salt.length];
            }
        }
        else
        {
            S = new byte[0];
        }

        byte[]  P;

        if ((password != null) && (password.length != 0))
        {
            P = new byte[v * ((password.length + v - 1) / v)];

            for (int i = 0; i != P.length; i++)
            {
                P[i] = password[i % password.length];
            }
        }
        else
        {
            P = new byte[0];
        }

        byte[]  I = new byte[S.length + P.length];

        System.arraycopy(S, 0, I, 0, S.length);
        System.arraycopy(P, 0, I, S.length, P.length);

        byte[]  B = new byte[v];
        int     c = (n + u - 1) / u;

        for (int i = 1; i <= c; i++)
        {
            byte[]  A = new byte[u];

            digest.update(D, 0, D.length);
            digest.update(I, 0, I.length);
            digest.doFinal(A, 0);
            for (int j = 1; j != iterationCount; j++)
            {
                digest.update(A, 0, A.length);
                digest.doFinal(A, 0);
            }

            for (int j = 0; j != B.length; j++)
            {
                B[j] = A[j % A.length];
            }

            for (int j = 0; j != I.length / v; j++)
            {
                adjust(I, j * v, B);
            }

            if (i == c)
            {
                System.arraycopy(A, 0, dKey, (i - 1) * u, dKey.length - ((i - 1) * u));
            }
            else
            {
                System.arraycopy(A, 0, dKey, (i - 1) * u, A.length);
            }
        }

        return dKey;
    }

    /**
     * Generate a key parameter derived from the password, salt, and iteration
     * count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
    public CipherParameters generateDerivedParameters(
        int keySize)
    {
        keySize = keySize / 8;

        byte[]  dKey = generateDerivedKey(KEY_MATERIAL, keySize);

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
     */
    public CipherParameters generateDerivedParameters(
        int     keySize,
        int     ivSize)
    {
        keySize = keySize / 8;
        ivSize = ivSize / 8;

        byte[]  dKey = generateDerivedKey(KEY_MATERIAL, keySize);

        byte[]  iv = generateDerivedKey(IV_MATERIAL, ivSize);

        return new ParametersWithIV(new KeyParameter(dKey, 0, keySize), iv, 0, ivSize);
    }

    /**
     * Generate a key parameter for use with a MAC derived from the password,
     * salt, and iteration count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
    public CipherParameters generateDerivedMacParameters(
        int keySize)
    {
        keySize = keySize / 8;

        byte[]  dKey = generateDerivedKey(MAC_MATERIAL, keySize);

        return new KeyParameter(dKey, 0, keySize);
    }
}
