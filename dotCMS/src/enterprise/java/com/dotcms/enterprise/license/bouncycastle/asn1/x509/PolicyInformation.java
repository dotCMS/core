/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class PolicyInformation
    extends ASN1Encodable
{
    private DERObjectIdentifier   policyIdentifier;
    private ASN1Sequence          policyQualifiers;

    public PolicyInformation(
        ASN1Sequence seq)
    {
        if (seq.size() < 1 || seq.size() > 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }

        policyIdentifier = DERObjectIdentifier.getInstance(seq.getObjectAt(0));

        if (seq.size() > 1)
        {
            policyQualifiers = ASN1Sequence.getInstance(seq.getObjectAt(1));
        }
    }

    public PolicyInformation(
        DERObjectIdentifier policyIdentifier)
    {
        this.policyIdentifier = policyIdentifier;
    }

    public PolicyInformation(
        DERObjectIdentifier policyIdentifier,
        ASN1Sequence        policyQualifiers)
    {
        this.policyIdentifier = policyIdentifier;
        this.policyQualifiers = policyQualifiers;
    }

    public static PolicyInformation getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof PolicyInformation)
        {
            return (PolicyInformation)obj;
        }

        return new PolicyInformation(ASN1Sequence.getInstance(obj));
    }

    public DERObjectIdentifier getPolicyIdentifier()
    {
        return policyIdentifier;
    }
    
    public ASN1Sequence getPolicyQualifiers()
    {
        return policyQualifiers;
    }
    
    /* 
     * PolicyInformation ::= SEQUENCE {
     *      policyIdentifier   CertPolicyId,
     *      policyQualifiers   SEQUENCE SIZE (1..MAX) OF
     *              PolicyQualifierInfo OPTIONAL }
     */ 
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(policyIdentifier);

        if (policyQualifiers != null)
        {
            v.add(policyQualifiers);
        }
        
        return new DERSequence(v);
    }
}
