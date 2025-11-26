/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.x509.qualified;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface RFC3739QCObjectIdentifiers
{
    //
    // base id
    //
    static final String                 id_qcs             = "1.3.6.1.5.5.7.11";

    static final DERObjectIdentifier    id_qcs_pkixQCSyntax_v1                = new DERObjectIdentifier(id_qcs+".1");
    static final DERObjectIdentifier    id_qcs_pkixQCSyntax_v2                 = new DERObjectIdentifier(id_qcs+".2");
}
