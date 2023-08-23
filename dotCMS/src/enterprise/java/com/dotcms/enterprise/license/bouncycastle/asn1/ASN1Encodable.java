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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base class for objects which can be written directly to ASN.1 output streams.
 */
public abstract class ASN1Encodable
    implements DEREncodable
{
    public static final String DER = "DER";
    public static final String BER = "BER";

    /**
     * Return the default BER or DER encoding for this object.
     *
     * @return BER/DER byte encoded object.
     * @throws IOException on encoding error.
     */
    public byte[] getEncoded() 
        throws IOException
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        ASN1OutputStream        aOut = new ASN1OutputStream(bOut);
        
        aOut.writeObject(this);
        
        return bOut.toByteArray();
    }

    /**
     * Return either the default for "BER" or a DER encoding if "DER" is specified.
     *
     * @param encoding name of encoding to use.
     * @return byte encoded object.
     * @throws IOException on encoding error.
     */
    public byte[] getEncoded(
        String encoding) 
        throws IOException
    {
        if (encoding.equals(DER))
        {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);
            
            dOut.writeObject(this);
            
            return bOut.toByteArray();
        }
        
        return this.getEncoded();
    }
    
    /**
     * Return the DER encoding of the object, null if the DER encoding can not be made.
     * 
     * @return a DER byte array, null otherwise.
     */
    public byte[] getDEREncoded()
    {
        try
        {
            return this.getEncoded(DER);
        }
        catch (IOException e)
        {
            return null;
        }
    }
    
    public int hashCode()
    {
        return this.toASN1Object().hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (this == o)
        {
            return true;
        }
        
        if (!(o instanceof DEREncodable))
        {
            return false;
        }

        DEREncodable other = (DEREncodable)o;

        return this.toASN1Object().equals(other.getDERObject());
    }

    public DERObject getDERObject()
    {        
        return this.toASN1Object();
    }

    public abstract DERObject toASN1Object();
}
