/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class EncryptionScheme
    extends AlgorithmIdentifier
{   
    DERObject   objectId;
    DERObject   obj;

    EncryptionScheme(
        ASN1Sequence  seq)
    {   
        super(seq);
        
        objectId = (DERObject)seq.getObjectAt(0);
        obj = (DERObject)seq.getObjectAt(1);
    }

    public DERObject getObject()
    {
        return obj;
    }

    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(objectId);
        v.add(obj);

        return new DERSequence(v);
    }
}
