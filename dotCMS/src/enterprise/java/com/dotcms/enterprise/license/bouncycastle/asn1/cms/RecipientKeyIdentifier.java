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

package com.dotcms.enterprise.license.bouncycastle.asn1.cms;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class RecipientKeyIdentifier
    extends ASN1Encodable
{
    private ASN1OctetString      subjectKeyIdentifier;
    private DERGeneralizedTime   date;
    private OtherKeyAttribute    other;

    public RecipientKeyIdentifier(
        ASN1OctetString         subjectKeyIdentifier,
        DERGeneralizedTime      date,
        OtherKeyAttribute       other)
    {
        this.subjectKeyIdentifier = subjectKeyIdentifier;
        this.date = date;
        this.other = other;
    }
    
    public RecipientKeyIdentifier(
        ASN1Sequence seq)
    {
        subjectKeyIdentifier = ASN1OctetString.getInstance(
                                                    seq.getObjectAt(0));
        
        switch(seq.size())
        {
        case 1:
            break;
        case 2:
            if (seq.getObjectAt(1) instanceof DERGeneralizedTime)
            {
                date = (DERGeneralizedTime)seq.getObjectAt(1); 
            }
            else
            {
                other = OtherKeyAttribute.getInstance(seq.getObjectAt(2));
            }
            break;
        case 3:
            date  = (DERGeneralizedTime)seq.getObjectAt(1);
            other = OtherKeyAttribute.getInstance(seq.getObjectAt(2));
            break;
        default:
            throw new IllegalArgumentException("Invalid RecipientKeyIdentifier");
        }
    }

    /**
     * return a RecipientKeyIdentifier object from a tagged object.
     *
     * @param _ato the tagged object holding the object we want.
     * @param _explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static RecipientKeyIdentifier getInstance(ASN1TaggedObject _ato, boolean _explicit)
    {
        return getInstance(ASN1Sequence.getInstance(_ato, _explicit));
    }
    
    /**
     * return a RecipientKeyIdentifier object from the given object.
     *
     * @param _obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static RecipientKeyIdentifier getInstance(Object _obj)
    {
        if(_obj == null || _obj instanceof RecipientKeyIdentifier)
        {
            return (RecipientKeyIdentifier)_obj;
        }
        
        if(_obj instanceof ASN1Sequence)
        {
            return new RecipientKeyIdentifier((ASN1Sequence)_obj);
        }
        
        throw new IllegalArgumentException("Invalid RecipientKeyIdentifier: " + _obj.getClass().getName());
    } 

    public ASN1OctetString getSubjectKeyIdentifier()
    {
        return subjectKeyIdentifier;
    }

    public DERGeneralizedTime getDate()
    {
        return date;
    }

    public OtherKeyAttribute getOtherKeyAttribute()
    {
        return other;
    }


    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * RecipientKeyIdentifier ::= SEQUENCE {
     *     subjectKeyIdentifier SubjectKeyIdentifier,
     *     date GeneralizedTime OPTIONAL,
     *     other OtherKeyAttribute OPTIONAL 
     * }
     *
     * SubjectKeyIdentifier ::= OCTET STRING
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(subjectKeyIdentifier);
        
        if (date != null)
        {
            v.add(date);
        }

        if (other != null)
        {
            v.add(other);
        }
        
        return new DERSequence(v);
    }
}
