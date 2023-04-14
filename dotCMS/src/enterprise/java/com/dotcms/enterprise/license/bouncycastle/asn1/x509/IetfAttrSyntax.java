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

import java.util.Enumeration;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTF8String;

/**
 * Implementation of <code>IetfAttrSyntax</code> as specified by RFC3281.
 */
public class IetfAttrSyntax
    extends ASN1Encodable
{
    public static final int VALUE_OCTETS    = 1;
    public static final int VALUE_OID       = 2;
    public static final int VALUE_UTF8      = 3;
    GeneralNames            policyAuthority = null;
    Vector                  values          = new Vector();
    int                     valueChoice     = -1;

    /**
     *  
     */
    public IetfAttrSyntax(ASN1Sequence seq)
    {
        int i = 0;

        if (seq.getObjectAt(0) instanceof ASN1TaggedObject)
        {
            policyAuthority = GeneralNames.getInstance(((ASN1TaggedObject)seq.getObjectAt(0)), false);
            i++;
        }
        else if (seq.size() == 2)
        { // VOMS fix
            policyAuthority = GeneralNames.getInstance(seq.getObjectAt(0));
            i++;
        }

        if (!(seq.getObjectAt(i) instanceof ASN1Sequence))
        {
            throw new IllegalArgumentException("Non-IetfAttrSyntax encoding");
        }

        seq = (ASN1Sequence)seq.getObjectAt(i);

        for (Enumeration e = seq.getObjects(); e.hasMoreElements();)
        {
            DERObject obj = (DERObject)e.nextElement();
            int type;

            if (obj instanceof DERObjectIdentifier)
            {
                type = VALUE_OID;
            }
            else if (obj instanceof DERUTF8String)
            {
                type = VALUE_UTF8;
            }
            else if (obj instanceof DEROctetString)
            {
                type = VALUE_OCTETS;
            }
            else
            {
                throw new IllegalArgumentException("Bad value type encoding IetfAttrSyntax");
            }

            if (valueChoice < 0)
            {
                valueChoice = type;
            }

            if (type != valueChoice)
            {
                throw new IllegalArgumentException("Mix of value types in IetfAttrSyntax");
            }

            values.addElement(obj);
        }
    }

    public GeneralNames getPolicyAuthority()
    {
        return policyAuthority;
    }

    public int getValueType()
    {
        return valueChoice;
    }

    public Object[] getValues()
    {
        if (this.getValueType() == VALUE_OCTETS)
        {
            ASN1OctetString[] tmp = new ASN1OctetString[values.size()];
            
            for (int i = 0; i != tmp.length; i++)
            {
                tmp[i] = (ASN1OctetString)values.elementAt(i);
            }
            
            return tmp;
        }
        else if (this.getValueType() == VALUE_OID)
        {
            DERObjectIdentifier[] tmp = new DERObjectIdentifier[values.size()];
            
            for (int i = 0; i != tmp.length; i++)
            {
                tmp[i] = (DERObjectIdentifier)values.elementAt(i);
            }
            
            return tmp;
        }
        else
        {
            DERUTF8String[] tmp = new DERUTF8String[values.size()];
            
            for (int i = 0; i != tmp.length; i++)
            {
                tmp[i] = (DERUTF8String)values.elementAt(i);
            }
            
            return tmp;
        }
    }

    /**
     * 
     * <pre>
     * 
     *  IetfAttrSyntax ::= SEQUENCE {
     *    policyAuthority [0] GeneralNames OPTIONAL,
     *    values SEQUENCE OF CHOICE {
     *      octets OCTET STRING,
     *      oid OBJECT IDENTIFIER,
     *      string UTF8String
     *    }
     *  }
     *  
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (policyAuthority != null)
        {
            v.add(new DERTaggedObject(0, policyAuthority));
        }

        ASN1EncodableVector v2 = new ASN1EncodableVector();

        for (Enumeration i = values.elements(); i.hasMoreElements();)
        {
            v2.add((ASN1Encodable)i.nextElement());
        }

        v.add(new DERSequence(v2));

        return new DERSequence(v);
    }
}
