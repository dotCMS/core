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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The extendedKeyUsage object.
 * <pre>
 *      extendedKeyUsage ::= SEQUENCE SIZE (1..MAX) OF KeyPurposeId
 * </pre>
 */
public class ExtendedKeyUsage
    extends ASN1Encodable
{
    Hashtable     usageTable = new Hashtable();
    ASN1Sequence  seq;

    public static ExtendedKeyUsage getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ExtendedKeyUsage getInstance(
        Object obj)
    {
        if (obj instanceof ExtendedKeyUsage) 
        {
            return (ExtendedKeyUsage)obj;
        }
        
        if(obj instanceof ASN1Sequence) 
        {
            return new ExtendedKeyUsage((ASN1Sequence)obj);
        }

        if (obj instanceof X509Extension)
        {
            return getInstance(X509Extension.convertValueToObject((X509Extension)obj));
        }

        throw new IllegalArgumentException("Invalid ExtendedKeyUsage: " + obj.getClass().getName());
    }

    public ExtendedKeyUsage(
        KeyPurposeId  usage)
    {
        this.seq = new DERSequence(usage);

        this.usageTable.put(usage, usage);
    }
    
    public ExtendedKeyUsage(
        ASN1Sequence  seq)
    {
        this.seq = seq;

        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            Object  o = e.nextElement();
            if (!(o instanceof DERObjectIdentifier))
            {
                throw new IllegalArgumentException("Only DERObjectIdentifiers allowed in ExtendedKeyUsage.");
            }
            this.usageTable.put(o, o);
        }
    }

    public ExtendedKeyUsage(
        Vector  usages)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        Enumeration         e = usages.elements();

        while (e.hasMoreElements())
        {
            DERObject  o = (DERObject)e.nextElement();

            v.add(o);
            this.usageTable.put(o, o);
        }

        this.seq = new DERSequence(v);
    }

    public boolean hasKeyPurposeId(
        KeyPurposeId keyPurposeId)
    {
        return (usageTable.get(keyPurposeId) != null);
    }
    
    /**
     * Returns all extended key usages.
     * The returned vector contains DERObjectIdentifiers.
     * @return A vector with all key purposes.
     */
    public Vector getUsages()
    {
        Vector temp = new Vector();
        for (Enumeration it = usageTable.elements(); it.hasMoreElements();)
        {
            temp.addElement(it.nextElement());
        }
        return temp;
    }

    public int size()
    {
        return usageTable.size();
    }
    
    public DERObject toASN1Object()
    {
        return seq;
    }
}
