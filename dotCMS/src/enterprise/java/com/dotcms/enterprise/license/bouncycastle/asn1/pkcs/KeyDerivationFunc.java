/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class KeyDerivationFunc
    extends AlgorithmIdentifier
{
    KeyDerivationFunc(
        ASN1Sequence  seq)
    {
        super(seq);
    }
    
    KeyDerivationFunc(
        DERObjectIdentifier id,
        ASN1Encodable       params)
    {
        super(id, params);
    }
}
