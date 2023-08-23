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

package com.dotcms.enterprise.license.bouncycastle.crypto.paddings;

import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;

/**
 * A padder that adds the padding according to the scheme referenced in
 * ISO 7814-4 - scheme 2 from ISO 9797-1. The first byte is 0x80, rest is 0x00
 */
public class ISO7816d4Padding
    implements BlockCipherPadding
{
    /**
     * Initialise the padder.
     *
     * @param random - a SecureRandom if available.
     */
    public void init(SecureRandom random)
        throws IllegalArgumentException
    {
        // nothing to do.
    }

    /**
     * Return the name of the algorithm the padder implements.
     *
     * @return the name of the algorithm the padder implements.
     */
    public String getPaddingName()
    {
        return "ISO7816-4";
    }

    /**
     * add the pad bytes to the passed in block, returning the
     * number of bytes added.
     */
    public int addPadding(
        byte[]  in,
        int     inOff)
    {
        int added = (in.length - inOff);

        in [inOff]= (byte) 0x80;
        inOff ++;
        
        while (inOff < in.length)
        {
            in[inOff] = (byte) 0;
            inOff++;
        }

        return added;
    }

    /**
     * return the number of pad bytes present in the block.
     */
    public int padCount(byte[] in)
        throws InvalidCipherTextException
    {
        int count = in.length - 1;

        while (count > 0 && in[count] == 0)
        {
            count--;
        }

        if (in[count] != (byte)0x80)
        {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        
        return in.length - count;
    }
}
