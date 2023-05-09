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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Basic type for a symmetric encrypted session key packet
 */
public class SymmetricKeyEncSessionPacket 
    extends ContainedPacket
{
    private int       version;
    private int       encAlgorithm;
    private S2K       s2k;
    private byte[]    secKeyData;
    
    public SymmetricKeyEncSessionPacket(
        BCPGInputStream  in)
        throws IOException
    {
        version = in.read();
        encAlgorithm = in.read();

        s2k = new S2K(in);
    
        if (in.available() != 0)
        {
            secKeyData = new byte[in.available()];
            in.readFully(secKeyData, 0, secKeyData.length);
        }
    }
    
    public SymmetricKeyEncSessionPacket(
        int       encAlgorithm,
        S2K       s2k,
        byte[]    secKeyData)
    {
        this.version = 4;
        this.encAlgorithm = encAlgorithm;
        this.s2k = s2k;
        this.secKeyData = secKeyData;
    }
    
    /**
     * @return int
     */
    public int getEncAlgorithm()
    {
        return encAlgorithm;
    }

    /**
     * @return S2K
     */
    public S2K getS2K()
    {
        return s2k;
    }

    /**
     * @return byte[]
     */
    public byte[] getSecKeyData()
    {
        return secKeyData;
    }

    /**
     * @return int
     */
    public int getVersion()
    {
        return version;
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        BCPGOutputStream        pOut = new BCPGOutputStream(bOut);

        pOut.write(version);
        pOut.write(encAlgorithm);
        pOut.writeObject(s2k);
        
        if (secKeyData != null && secKeyData.length > 0)
        {
            pOut.write(secKeyData);
        }
        
        out.writePacket(SYMMETRIC_KEY_ENC_SESSION, bOut.toByteArray(), true);
    }
}
