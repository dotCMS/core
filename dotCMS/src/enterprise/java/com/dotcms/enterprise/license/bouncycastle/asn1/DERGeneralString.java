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

public class DERGeneralString 
    extends ASN1Object implements DERString
{
    private String string;

    public static DERGeneralString getInstance(
        Object obj) 
    {
        if (obj == null || obj instanceof DERGeneralString) 
        {
            return (DERGeneralString) obj;
        }
        if (obj instanceof ASN1OctetString) 
        {
            return new DERGeneralString(((ASN1OctetString) obj).getOctets());
        }
        if (obj instanceof ASN1TaggedObject) 
        {
            return getInstance(((ASN1TaggedObject) obj).getObject());
        }
        throw new IllegalArgumentException("illegal object in getInstance: "
                + obj.getClass().getName());
    }

    public static DERGeneralString getInstance(
        ASN1TaggedObject obj, 
        boolean explicit) 
    {
        return getInstance(obj.getObject());
    }

    public DERGeneralString(byte[] string) 
    {
        char[] cs = new char[string.length];
        for (int i = 0; i != cs.length; i++)
        {
            cs[i] = (char)(string[i] & 0xff);
        }
        this.string = new String(cs);
    }

    public DERGeneralString(String string) 
    {
        this.string = string;
    }
    
    public String getString() 
    {
        return string;
    }

    public String toString()
    {
        return string;
    }

    public byte[] getOctets() 
    {
        char[] cs = string.toCharArray();
        byte[] bs = new byte[cs.length];
        for (int i = 0; i != cs.length; i++) 
        {
            bs[i] = (byte) cs[i];
        }
        return bs;
    }
    
    void encode(DEROutputStream out) 
        throws IOException 
    {
        out.writeEncoded(GENERAL_STRING, this.getOctets());
    }
    
    public int hashCode() 
    {
        return this.getString().hashCode();
    }
    
    boolean asn1Equals(DERObject o)
    {
        if (!(o instanceof DERGeneralString)) 
        {
            return false;
        }
        DERGeneralString s = (DERGeneralString) o;
        return this.getString().equals(s.getString());
    }
}
