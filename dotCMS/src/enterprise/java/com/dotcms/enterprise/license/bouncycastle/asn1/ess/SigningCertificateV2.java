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

package com.dotcms.enterprise.license.bouncycastle.asn1.ess;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.PolicyInformation;

public class SigningCertificateV2
    extends ASN1Encodable
{
    ASN1Sequence certs;
    ASN1Sequence policies;

    public static SigningCertificateV2 getInstance(
        Object o)
    {
        if (o == null || o instanceof SigningCertificateV2)
        {
            return (SigningCertificateV2) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new SigningCertificateV2((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SigningCertificateV2' factory : "
                        + o.getClass().getName() + ".");
    }

    public SigningCertificateV2(
        ASN1Sequence seq)
    {
        if (seq.size() < 1 || seq.size() > 2)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        this.certs = ASN1Sequence.getInstance(seq.getObjectAt(0));

        if (seq.size() > 1)
        {
            this.policies = ASN1Sequence.getInstance(seq.getObjectAt(1));
        }
    }

    public SigningCertificateV2(
        ESSCertIDv2[] certs)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i=0; i < certs.length; i++)
        {
            v.add(certs[i]);
        }
        this.certs = new DERSequence(v);
    }

    public SigningCertificateV2(
        ESSCertIDv2[] certs,
        PolicyInformation[] policies)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i=0; i < certs.length; i++)
        {
            v.add(certs[i]);
        }
        this.certs = new DERSequence(v);

        if (policies != null)
        {
            v = new ASN1EncodableVector();
            for (int i=0; i < policies.length; i++)
            {
                v.add(policies[i]);
            }
            this.policies = new DERSequence(v);
        }
    }

    public ESSCertIDv2[] getCerts()
    {
        ESSCertIDv2[] certIds = new ESSCertIDv2[certs.size()];
        for (int i = 0; i != certs.size(); i++)
        {
            certIds[i] = ESSCertIDv2.getInstance(certs.getObjectAt(i));
        }
        return certIds;
    }

    public PolicyInformation[] getPolicies()
    {
        if (policies == null)
        {
            return null;
        }

        PolicyInformation[] policyInformations = new PolicyInformation[policies.size()];
        for (int i = 0; i != policies.size(); i++)
        {
            policyInformations[i] = PolicyInformation.getInstance(policies.getObjectAt(i));
        }
        return policyInformations;
    }

    /**
     * The definition of SigningCertificateV2 is
     * <pre>
     * SigningCertificateV2 ::=  SEQUENCE {
     *      certs        SEQUENCE OF ESSCertIDv2,
     *      policies     SEQUENCE OF PolicyInformation OPTIONAL
     * }
     * </pre>
     * id-aa-signingCertificateV2 OBJECT IDENTIFIER ::= { iso(1)
     *    member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs9(9)
     *    smime(16) id-aa(2) 47 }
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(certs);

        if (policies != null)
        {
            v.add(policies);
        }

        return new DERSequence(v);
    }
}
