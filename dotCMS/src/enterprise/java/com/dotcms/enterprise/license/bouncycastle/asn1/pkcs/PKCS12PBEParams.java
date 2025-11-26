/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import java.math.BigInteger;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class PKCS12PBEParams
    extends ASN1Encodable
{
    DERInteger      iterations;
    ASN1OctetString iv;

    public PKCS12PBEParams(
        byte[]      salt,
        int         iterations)
    {
        this.iv = new DEROctetString(salt);
        this.iterations = new DERInteger(iterations);
    }

    public PKCS12PBEParams(
        ASN1Sequence  seq)
    {
        iv = (ASN1OctetString)seq.getObjectAt(0);
        iterations = (DERInteger)seq.getObjectAt(1);
    }

    public static PKCS12PBEParams getInstance(
        Object  obj)
    {
        if (obj instanceof PKCS12PBEParams)
        {
            return (PKCS12PBEParams)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new PKCS12PBEParams((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public BigInteger getIterations()
    {
        return iterations.getValue();
    }

    public byte[] getIV()
    {
        return iv.getOctets();
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(iv);
        v.add(iterations);

        return new DERSequence(v);
    }
}
