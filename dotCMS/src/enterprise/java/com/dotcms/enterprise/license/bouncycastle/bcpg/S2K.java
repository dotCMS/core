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
 * The string to key specifier class
 */
public class S2K 
    extends BCPGObject
{
    private static final int EXPBIAS = 6;
    
    public static final int SIMPLE = 0;
    public static final int SALTED = 1;
    public static final int SALTED_AND_ITERATED = 3;
    public static final int GNU_DUMMY_S2K = 101;
    
    int       type;
    int       algorithm;
    byte[]    iv;
    int       itCount = -1;
    int       protectionMode = -1;
    
    S2K(
        InputStream    in)
        throws IOException
    {
        DataInputStream    dIn = new DataInputStream(in);
        
        type = dIn.read();
        algorithm = dIn.read();
        
        //
        // if this happens we have a dummy-S2K packet.
        //
        if (type != GNU_DUMMY_S2K)
        {
            if (type != 0)
            {
                iv = new byte[8];
                dIn.readFully(iv, 0, iv.length);

                if (type == 3)
                {
                    itCount = dIn.read();
                }
            }
        }
        else
        {
            dIn.read(); // G
            dIn.read(); // N
            dIn.read(); // U
            protectionMode = dIn.read(); // protection mode
        }
    }
    
    public S2K(
        int        algorithm)
    {
        this.type = 0;
        this.algorithm = algorithm;
    }
    
    public S2K(
        int        algorithm,
        byte[]    iv)
    {
        this.type = 1;
        this.algorithm = algorithm;
        this.iv = iv;
    }

    public S2K(
        int       algorithm,
        byte[]    iv,
        int       itCount)
    {
        this.type = 3;
        this.algorithm = algorithm;
        this.iv = iv;
        this.itCount = itCount;
    }
    
    public int getType()
    {
        return type;
    }
    
    /**
     * return the hash algorithm for this S2K
     */
    public int getHashAlgorithm()
    {
        return algorithm;
    }
    
    /**
     * return the iv for the key generation algorithm
     */
    public byte[] getIV()
    {
        return iv;
    }
    
    /**
     * return the iteration count
     */
    public long getIterationCount()
    {
        return (16 + (itCount & 15)) << ((itCount >> 4) + EXPBIAS);
    }
    
    /**
     * the protection mode - only if GNU_DUMMY_S2K
     */
    public int getProtectionMode()
    {
        return protectionMode;
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.write(type);
        out.write(algorithm);
    
        if (type != GNU_DUMMY_S2K)
        {
            if (type != 0)
            {
                out.write(iv);
            }
            
            if (type == 3)
            {
                out.write(itCount);
            }
        }
        else
        {
            out.write('G');
            out.write('N');
            out.write('U');
            out.write(protectionMode);
        }
    }
}
