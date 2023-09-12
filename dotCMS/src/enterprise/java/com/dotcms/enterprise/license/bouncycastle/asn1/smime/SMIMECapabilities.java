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

package com.dotcms.enterprise.license.bouncycastle.asn1.smime;

import java.util.Enumeration;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.cms.Attribute;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * Handler class for dealing with S/MIME Capabilities
 */
public class SMIMECapabilities
    extends ASN1Encodable
{
    /**
     * general preferences
     */
    public static final DERObjectIdentifier preferSignedData = PKCSObjectIdentifiers.preferSignedData;
    public static final DERObjectIdentifier canNotDecryptAny = PKCSObjectIdentifiers.canNotDecryptAny;
    public static final DERObjectIdentifier sMIMECapabilitesVersions = PKCSObjectIdentifiers.sMIMECapabilitiesVersions;

    /**
     * encryption algorithms preferences
     */
    public static final DERObjectIdentifier dES_CBC = new DERObjectIdentifier("1.3.14.3.2.7");
    public static final DERObjectIdentifier dES_EDE3_CBC = PKCSObjectIdentifiers.des_EDE3_CBC;
    public static final DERObjectIdentifier rC2_CBC = PKCSObjectIdentifiers.RC2_CBC;
    
    private ASN1Sequence         capabilities;

    /**
     * return an Attribute object from the given object.
     *
     * @param o the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static SMIMECapabilities getInstance(
        Object o)
    {
        if (o == null || o instanceof SMIMECapabilities)
        {
            return (SMIMECapabilities)o;
        }
        
        if (o instanceof ASN1Sequence)
        {
            return new SMIMECapabilities((ASN1Sequence)o);
        }

        if (o instanceof Attribute)
        {
            return new SMIMECapabilities(
                (ASN1Sequence)(((Attribute)o).getAttrValues().getObjectAt(0)));
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    
    public SMIMECapabilities(
        ASN1Sequence seq)
    {
        capabilities = seq;
    }

    /**
     * returns a vector with 0 or more objects of all the capabilities
     * matching the passed in capability OID. If the OID passed is null the
     * entire set is returned.
     */
    public Vector getCapabilities(
        DERObjectIdentifier capability)
    {
        Enumeration e = capabilities.getObjects();
        Vector      list = new Vector();

        if (capability == null)
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());

                list.addElement(cap);
            }
        }
        else
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());

                if (capability.equals(cap.getCapabilityID()))
                {
                    list.addElement(cap);
                }
            }
        }

        return list;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SMIMECapabilities ::= SEQUENCE OF SMIMECapability
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return capabilities;
    }
}
