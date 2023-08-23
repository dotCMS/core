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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Generator for X.509 extensions
 */
public class X509ExtensionsGenerator
{
    private Hashtable extensions = new Hashtable();
    private Vector extOrdering = new Vector();

    /**
     * Reset the generator
     */
    public void reset()
    {
        extensions = new Hashtable();
        extOrdering = new Vector();
    }

    /**
     * Add an extension with the given oid and the passed in value to be included
     * in the OCTET STRING associated with the extension.
     *
     * @param oid  OID for the extension.
     * @param critical  true if critical, false otherwise.
     * @param value the ASN.1 object to be included in the extension.
     */
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        DEREncodable        value)
    {
        try
        {
            this.addExtension(oid, critical, value.getDERObject().getEncoded(ASN1Encodable.DER));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("error encoding value: " + e);
        }
    }

    /**
     * Add an extension with the given oid and the passed in byte array to be wrapped in the
     * OCTET STRING associated with the extension.
     *
     * @param oid OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value the byte array to be wrapped.
     */
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        byte[]              value)
    {
        if (extensions.containsKey(oid))
        {
            throw new IllegalArgumentException("extension " + oid + " already added");
        }

        extOrdering.addElement(oid);
        extensions.put(oid, new X509Extension(critical, new DEROctetString(value)));
    }

    /**
     * Return true if there are no extension present in this generator.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty()
    {
        return extOrdering.isEmpty();
    }

    /**
     * Generate an X509Extensions object based on the current state of the generator.
     *
     * @return  an X09Extensions object.
     */
    public X509Extensions generate()
    {
        return new X509Extensions(extOrdering, extensions);
    }
}
