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

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;

/**
 * basic packet for a PGP secret key
 */
public class SecretKeyPacket 
    extends ContainedPacket implements PublicKeyAlgorithmTags
{
    public static final int USAGE_NONE = 0x00;
    public static final int USAGE_CHECKSUM = 0xff;
    public static final int USAGE_SHA1 = 0xfe;

    private PublicKeyPacket    pubKeyPacket;
    private byte[]             secKeyData;
    private int                s2kUsage;
    private int                encAlgorithm;
    private S2K                s2k;
    private byte[]             iv;
    
    /**
     * 
     * @param in
     * @throws IOException
     */
    SecretKeyPacket(
        BCPGInputStream    in)
        throws IOException
    {
        if (this instanceof SecretSubkeyPacket)
        {
            pubKeyPacket = new PublicSubkeyPacket(in);
        }
        else
        {
            pubKeyPacket = new PublicKeyPacket(in);
        }

        s2kUsage = in.read();

        if (s2kUsage == USAGE_CHECKSUM || s2kUsage == USAGE_SHA1)
        {
            encAlgorithm = in.read();
            s2k = new S2K(in);
        }
        else
        {
            encAlgorithm = s2kUsage;
        }

        if (!(s2k != null && s2k.getType() == S2K.GNU_DUMMY_S2K && s2k.getProtectionMode() == 0x01))
        {
            if (s2kUsage != 0) 
            {
                if (encAlgorithm < 7)
                {
                    iv = new byte[8];
                }
                else
                {
                    iv = new byte[16];
                }
                in.readFully(iv, 0, iv.length);
            }
        }
        
        if (in.available() != 0)
        {
            secKeyData = new byte[in.available()];
            
            in.readFully(secKeyData);
        }
    }
    
    /**
     * 
     * @param pubKeyPacket
     * @param encAlgorithm
     * @param s2k
     * @param iv
     * @param secKeyData
     */
    public SecretKeyPacket(
        PublicKeyPacket pubKeyPacket,
        int             encAlgorithm,
        S2K             s2k,
        byte[]          iv,
        byte[]          secKeyData)
    {
        this.pubKeyPacket = pubKeyPacket;
        this.encAlgorithm = encAlgorithm;
        
        if (encAlgorithm != SymmetricKeyAlgorithmTags.NULL)
        {
            this.s2kUsage = USAGE_CHECKSUM;
        }
        else
        {
            this.s2kUsage = USAGE_NONE;
        }
        
        this.s2k = s2k;
        this.iv = iv;
        this.secKeyData = secKeyData;
    }
    
    public SecretKeyPacket(
        PublicKeyPacket pubKeyPacket,
        int             encAlgorithm,
        int             s2kUsage,
        S2K             s2k,
        byte[]          iv,
        byte[]          secKeyData)
    {
        this.pubKeyPacket = pubKeyPacket;
        this.encAlgorithm = encAlgorithm;
        this.s2kUsage = s2kUsage;
        this.s2k = s2k;
        this.iv = iv;
        this.secKeyData = secKeyData;
    }

    public int getEncAlgorithm()
    {
        return encAlgorithm;
    }
    
    public int getS2KUsage()
    {
        return s2kUsage;
    }

    public byte[] getIV()
    {
        return iv;
    }
    
    public S2K getS2K()
    {
        return s2k;
    }
    
    public PublicKeyPacket getPublicKeyPacket()
    {
        return pubKeyPacket;
    }
    
    public byte[] getSecretKeyData()
    {
        return secKeyData;
    }
    
    public byte[] getEncodedContents()
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        BCPGOutputStream         pOut = new BCPGOutputStream(bOut);
        
        pOut.write(pubKeyPacket.getEncodedContents());
        
        pOut.write(s2kUsage);

        if (s2kUsage == USAGE_CHECKSUM || s2kUsage == USAGE_SHA1)
        {
            pOut.write(encAlgorithm);
            pOut.writeObject(s2k);
        }
        
        if (iv != null)
        {
            pOut.write(iv);
        }
        
        if (secKeyData != null && secKeyData.length > 0)
        {
            pOut.write(secKeyData);
        }
        
        return bOut.toByteArray();
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(SECRET_KEY, getEncodedContents(), true);
    }
}
