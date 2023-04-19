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

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.IOException;

import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * We insert one of these when we find a tag we don't recognise.
 */
public class DERUnknownTag
    extends DERObject
{
    private boolean   isConstructed;
    private int       tag;
    private byte[]    data;

    /**
     * @param tag the tag value.
     * @param data the contents octets.
     */
    public DERUnknownTag(
        int     tag,
        byte[]  data)
    {
        this(false, tag, data);
    }

    public DERUnknownTag(
        boolean isConstructed,
        int     tag,
        byte[]  data)
    {
        this.isConstructed = isConstructed;
        this.tag = tag;
        this.data = data;
    }

    public boolean isConstructed()
    {
        return isConstructed;
    }

    public int getTag()
    {
        return tag;
    }

    public byte[] getData()
    {
        return data;
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(isConstructed ? DERTags.CONSTRUCTED : 0, tag, data);
    }
    
    public boolean equals(
        Object o)
    {
        if (!(o instanceof DERUnknownTag))
        {
            return false;
        }
        
        DERUnknownTag other = (DERUnknownTag)o;

        return isConstructed == other.isConstructed
            && tag == other.tag
            && Arrays.areEqual(data, other.data);
    }
    
    public int hashCode()
    {
        return (isConstructed ? ~0 : 0) ^ tag ^ Arrays.hashCode(data);
    }
}
