/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cmp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

public class CertRepMessage
    extends ASN1Encodable
{
    private ASN1Sequence caPubs;
    private ASN1Sequence response;

    private CertRepMessage(ASN1Sequence seq)
    {
        int index = 0;

        if (seq.size() > 1)
        {
            caPubs = ASN1Sequence.getInstance((ASN1TaggedObject)seq.getObjectAt(index++), true);
        }

        response = ASN1Sequence.getInstance(seq.getObjectAt(index));
    }

    public static CertRepMessage getInstance(Object o)
    {
        if (o instanceof CertRepMessage)
        {
            return (CertRepMessage)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CertRepMessage((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public CMPCertificate[] getCaPubs()
    {
        if (caPubs == null)
        {
            return null;
        }

        CMPCertificate[] results = new CMPCertificate[caPubs.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CMPCertificate.getInstance(caPubs.getObjectAt(i));
        }

        return results;
    }

    public CertResponse[] getResponse()
    {
        CertResponse[] results = new CertResponse[response.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CertResponse.getInstance(response.getObjectAt(i));
        }

        return results;
    }

    /**
     * <pre>
     * CertRepMessage ::= SEQUENCE {
     *                          caPubs       [1] SEQUENCE SIZE (1..MAX) OF CMPCertificate
     *                                                                             OPTIONAL,
     *                          response         SEQUENCE OF CertResponse
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (caPubs != null)
        {
            v.add(new DERTaggedObject(true, 1, caPubs));
        }

        v.add(response);

        return new DERSequence(v);
    }
}
