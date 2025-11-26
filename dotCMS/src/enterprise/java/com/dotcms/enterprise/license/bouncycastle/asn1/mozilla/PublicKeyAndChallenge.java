/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.mozilla;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * This is designed to parse
 * the PublicKeyAndChallenge created by the KEYGEN tag included by
 * Mozilla based browsers.
 *  <pre>
 *  PublicKeyAndChallenge ::= SEQUENCE {
 *    spki SubjectPublicKeyInfo,
 *    challenge IA5STRING
 *  }
 *
 *  </pre>
 */
public class PublicKeyAndChallenge
    extends ASN1Encodable
{
    private ASN1Sequence         pkacSeq;
    private SubjectPublicKeyInfo spki;
    private DERIA5String         challenge;

    public static PublicKeyAndChallenge getInstance(Object obj)
    {
        if (obj instanceof PublicKeyAndChallenge)
        {
            return (PublicKeyAndChallenge)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new PublicKeyAndChallenge((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unkown object in factory: " + obj.getClass().getName());
    }

    public PublicKeyAndChallenge(ASN1Sequence seq)
    {
        pkacSeq = seq;
        spki = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(0));
        challenge = DERIA5String.getInstance(seq.getObjectAt(1));
    }

    public DERObject toASN1Object()
    {
        return pkacSeq;
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
    {
        return spki;
    }

    public DERIA5String getChallenge()
    {
        return challenge;
    }
}
