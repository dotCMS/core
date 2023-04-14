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
 * generic signature object
 */
public class OnePassSignaturePacket 
    extends ContainedPacket
{
    private int  version;
    private int  sigType;
    private int  hashAlgorithm;
    private int  keyAlgorithm;
    private long keyID;
    private int  nested;
    
    OnePassSignaturePacket(
        BCPGInputStream    in)
        throws IOException
    {
        version = in.read();
        sigType = in.read();
        hashAlgorithm = in.read();
        keyAlgorithm = in.read();
        
        keyID |= (long)in.read() << 56;
        keyID |= (long)in.read() << 48;
        keyID |= (long)in.read() << 40;
        keyID |= (long)in.read() << 32;
        keyID |= (long)in.read() << 24;
        keyID |= (long)in.read() << 16;
        keyID |= (long)in.read() << 8;
        keyID |= in.read();
        
        nested = in.read();
    }
    
    public OnePassSignaturePacket(
        int        sigType,
        int        hashAlgorithm,
        int        keyAlgorithm,
        long       keyID,
        boolean    isNested)
    {
        this.version = 3;
        this.sigType = sigType;
        this.hashAlgorithm = hashAlgorithm;
        this.keyAlgorithm = keyAlgorithm;
        this.keyID = keyID;
        this.nested = (isNested) ? 0 : 1;
    }
    
    /**
     * Return the signature type.
     * @return the signature type
     */
    public int getSignatureType()
    {
        return sigType;
    }
    
    /**
     * return the encryption algorithm tag
     */
    public int getKeyAlgorithm()
    {
        return keyAlgorithm;
    }
    
    /**
     * return the hashAlgorithm tag
     */
    public int getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    
    /**
     * @return long
     */
    public long getKeyID()
    {
        return keyID;
    }
    
    /**
     * 
     */
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        BCPGOutputStream            pOut = new BCPGOutputStream(bOut);
  
        pOut.write(version);
        pOut.write(sigType);
        pOut.write(hashAlgorithm);
        pOut.write(keyAlgorithm);

        pOut.write((byte)(keyID >> 56));
        pOut.write((byte)(keyID >> 48));
        pOut.write((byte)(keyID >> 40));
        pOut.write((byte)(keyID >> 32));
        pOut.write((byte)(keyID >> 24));
        pOut.write((byte)(keyID >> 16));
        pOut.write((byte)(keyID >> 8));
        pOut.write((byte)(keyID));
        
        pOut.write(nested);
        
        out.writePacket(ONE_PASS_SIGNATURE, bOut.toByteArray(), true);
    }
}
