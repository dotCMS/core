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

package com.dotcms.enterprise.license.bouncycastle.crypto.macs;

import java.util.Hashtable;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.ExtendedDigest;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

/**
 * HMAC implementation based on RFC2104
 *
 * H(K XOR opad, H(K XOR ipad, text))
 */
public class HMac
    implements Mac
{
    private final static byte IPAD = (byte)0x36;
    private final static byte OPAD = (byte)0x5C;

    private Digest digest;
    private int digestSize;
    private int blockLength;
    
    private byte[] inputPad;
    private byte[] outputPad;

    private static Hashtable blockLengths;
    
    static
    {
        blockLengths = new Hashtable();
        
        blockLengths.put("GOST3411", Integer.valueOf(32));
        
        blockLengths.put("MD2", Integer.valueOf(16));
        blockLengths.put("MD4", Integer.valueOf(64));
        blockLengths.put("MD5", Integer.valueOf(64));
        
        blockLengths.put("RIPEMD128", Integer.valueOf(64));
        blockLengths.put("RIPEMD160", Integer.valueOf(64));
        
        blockLengths.put("SHA-1", Integer.valueOf(64));
        blockLengths.put("SHA-224", Integer.valueOf(64));
        blockLengths.put("SHA-256", Integer.valueOf(64));
        blockLengths.put("SHA-384", Integer.valueOf(128));
        blockLengths.put("SHA-512", Integer.valueOf(128));
        
        blockLengths.put("Tiger", Integer.valueOf(64));
        blockLengths.put("Whirlpool", Integer.valueOf(64));
    }
    
    private static int getByteLength(
        Digest digest)
    {
        if (digest instanceof ExtendedDigest)
        {
            return ((ExtendedDigest)digest).getByteLength();
        }
        
        Integer  b = (Integer)blockLengths.get(digest.getAlgorithmName());
        
        if (b == null)
        {       
            throw new IllegalArgumentException("unknown digest passed: " + digest.getAlgorithmName());
        }
        
        return b.intValue();
    }
    
    /**
     * Base constructor for one of the standard digest algorithms that the 
     * byteLength of the algorithm is know for.
     * 
     * @param digest the digest.
     */
    public HMac(
        Digest digest)
    {
        this(digest, getByteLength(digest));
    }

    private HMac(
        Digest digest,
        int    byteLength)
    {
        this.digest = digest;
        digestSize = digest.getDigestSize();

        this.blockLength = byteLength;

        inputPad = new byte[blockLength];
        outputPad = new byte[blockLength];
    }
    
    public String getAlgorithmName()
    {
        return digest.getAlgorithmName() + "/HMAC";
    }

    public Digest getUnderlyingDigest()
    {
        return digest;
    }

    public void init(
        CipherParameters params)
    {
        digest.reset();

        byte[] key = ((KeyParameter)params).getKey();

        if (key.length > blockLength)
        {
            digest.update(key, 0, key.length);
            digest.doFinal(inputPad, 0);
            for (int i = digestSize; i < inputPad.length; i++)
            {
                inputPad[i] = 0;
            }
        }
        else
        {
            System.arraycopy(key, 0, inputPad, 0, key.length);
            for (int i = key.length; i < inputPad.length; i++)
            {
                inputPad[i] = 0;
            }
        }

        outputPad = new byte[inputPad.length];
        System.arraycopy(inputPad, 0, outputPad, 0, inputPad.length);

        for (int i = 0; i < inputPad.length; i++)
        {
            inputPad[i] ^= IPAD;
        }

        for (int i = 0; i < outputPad.length; i++)
        {
            outputPad[i] ^= OPAD;
        }

        digest.update(inputPad, 0, inputPad.length);
    }

    public int getMacSize()
    {
        return digestSize;
    }

    public void update(
        byte in)
    {
        digest.update(in);
    }

    public void update(
        byte[] in,
        int inOff,
        int len)
    {
        digest.update(in, inOff, len);
    }

    public int doFinal(
        byte[] out,
        int outOff)
    {
        byte[] tmp = new byte[digestSize];
        digest.doFinal(tmp, 0);

        digest.update(outputPad, 0, outputPad.length);
        digest.update(tmp, 0, tmp.length);

        int     len = digest.doFinal(out, outOff);

        reset();

        return len;
    }

    /**
     * Reset the mac generator.
     */
    public void reset()
    {
        /*
         * reset the underlying digest.
         */
        digest.reset();

        /*
         * reinitialize the digest.
         */
        digest.update(inputPad, 0, inputPad.length);
    }
}
