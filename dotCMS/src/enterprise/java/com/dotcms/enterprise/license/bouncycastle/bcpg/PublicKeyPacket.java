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
import java.util.Date;

/**
 * basic packet for a PGP public key
 */
public class PublicKeyPacket 
    extends ContainedPacket implements PublicKeyAlgorithmTags
{
    private int            version;
    private long           time;
    private int            validDays;
    private int            algorithm;
    private BCPGKey        key;
    
    PublicKeyPacket(
        BCPGInputStream    in)
        throws IOException
    {      
        version = in.read();
        time = ((long)in.read() << 24) | (in.read() << 16) | (in.read() << 8) | in.read();
 
        if (version <= 3)
        {
            validDays = (in.read() << 8) | in.read();
        }
        
        algorithm = (byte)in.read();

        switch (algorithm)
        {
        case RSA_ENCRYPT:
        case RSA_GENERAL:
        case RSA_SIGN:
            key = new RSAPublicBCPGKey(in);
            break;
        case DSA:
            key = new DSAPublicBCPGKey(in);
            break;
        case ELGAMAL_ENCRYPT:
        case ELGAMAL_GENERAL:
            key = new ElGamalPublicBCPGKey(in);
            break;
        default:
            throw new IOException("unknown PGP public key algorithm encountered");
        }
    }
    
    /**
     * Construct version 4 public key packet.
     * 
     * @param algorithm
     * @param time
     * @param key
     */
    public PublicKeyPacket(
        int        algorithm,
        Date       time,
        BCPGKey    key)
    {
        this.version = 4;
        this.time = time.getTime() / 1000;
        this.algorithm = algorithm;
        this.key = key;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public int getAlgorithm()
    {
        return algorithm;
    }
    
    public int getValidDays()
    {
        return validDays;
    }
    
    public Date getTime()
    {
        return new Date(time * 1000);
    }
    
    public BCPGKey getKey()
    {
        return key;
    }
    
    public byte[] getEncodedContents() 
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        BCPGOutputStream         pOut = new BCPGOutputStream(bOut);
    
        pOut.write(version);
    
        pOut.write((byte)(time >> 24));
        pOut.write((byte)(time >> 16));
        pOut.write((byte)(time >> 8));
        pOut.write((byte)time);
    
        if (version <= 3)
        {
            pOut.write((byte)(validDays >> 8));
            pOut.write((byte)validDays);
        }
    
        pOut.write(algorithm);
    
        pOut.writeObject((BCPGObject)key);
    
        return bOut.toByteArray();
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(PUBLIC_KEY, getEncodedContents(), true);
    }
}
