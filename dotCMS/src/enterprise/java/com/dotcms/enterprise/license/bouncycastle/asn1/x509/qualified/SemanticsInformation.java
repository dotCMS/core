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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509.qualified;

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;

/**
 * The SemanticsInformation object.
 * <pre>
 *       SemanticsInformation ::= SEQUENCE {
 *         semanticsIdentifier        OBJECT IDENTIFIER   OPTIONAL,
 *         nameRegistrationAuthorities NameRegistrationAuthorities
 *                                                         OPTIONAL }
 *         (WITH COMPONENTS {..., semanticsIdentifier PRESENT}|
 *          WITH COMPONENTS {..., nameRegistrationAuthorities PRESENT})
 *
 *     NameRegistrationAuthorities ::=  SEQUENCE SIZE (1..MAX) OF
 *         GeneralName
 * </pre>
 */
public class SemanticsInformation extends ASN1Encodable
{
    DERObjectIdentifier semanticsIdentifier;
    GeneralName[] nameRegistrationAuthorities;    
    
    public static SemanticsInformation getInstance(Object obj)
    {
        if (obj == null || obj instanceof SemanticsInformation)
        {
            return (SemanticsInformation)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new SemanticsInformation(ASN1Sequence.getInstance(obj));            
        }
        
        throw new IllegalArgumentException("unknown object in getInstance");
    }
        
    public SemanticsInformation(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        if (seq.size() < 1)
        {
             throw new IllegalArgumentException("no objects in SemanticsInformation");
        }
        
        Object object = e.nextElement();
        if (object instanceof DERObjectIdentifier)
        {
            semanticsIdentifier = DERObjectIdentifier.getInstance(object);
            if (e.hasMoreElements())
            {
                object = e.nextElement();
            }
            else
            {
                object = null;
            }
        }
        
        if (object != null)
        {
            ASN1Sequence generalNameSeq = ASN1Sequence.getInstance(object);
            nameRegistrationAuthorities = new GeneralName[generalNameSeq.size()];
            for (int i= 0; i < generalNameSeq.size(); i++)
            {
                nameRegistrationAuthorities[i] = GeneralName.getInstance(generalNameSeq.getObjectAt(i));
            } 
        }
    }
        
    public SemanticsInformation(
        DERObjectIdentifier semanticsIdentifier,
        GeneralName[] generalNames)
    {
        this.semanticsIdentifier = semanticsIdentifier;
        this.nameRegistrationAuthorities = generalNames;
    }

    public SemanticsInformation(DERObjectIdentifier semanticsIdentifier)
    {
        this.semanticsIdentifier = semanticsIdentifier;
        this.nameRegistrationAuthorities = null;
    }

    public SemanticsInformation(GeneralName[] generalNames)
    {
        this.semanticsIdentifier = null;
        this.nameRegistrationAuthorities = generalNames;
    }        
    
    public DERObjectIdentifier getSemanticsIdentifier()
    {
        return semanticsIdentifier;
    }
        
    public GeneralName[] getNameRegistrationAuthorities()
    {
        return nameRegistrationAuthorities;
    } 
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        
        if (this.semanticsIdentifier != null)
        {
            seq.add(semanticsIdentifier);
        }
        if (this.nameRegistrationAuthorities != null)
        {
            ASN1EncodableVector seqname = new ASN1EncodableVector();
            for (int i = 0; i < nameRegistrationAuthorities.length; i++) 
            {
                seqname.add(nameRegistrationAuthorities[i]);
            }            
            seq.add(new DERSequence(seqname));
        }            
        
        return new DERSequence(seq);
    }                   
}
