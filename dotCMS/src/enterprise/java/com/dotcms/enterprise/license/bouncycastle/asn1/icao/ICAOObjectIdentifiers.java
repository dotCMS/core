/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.icao;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface ICAOObjectIdentifiers
{
    //
    // base id
    //
    static final String                 id_icao                   = "2.23.136";

    static final DERObjectIdentifier    id_icao_mrtd              = new DERObjectIdentifier(id_icao+".1");
    static final DERObjectIdentifier    id_icao_mrtd_security     = new DERObjectIdentifier(id_icao_mrtd+".1");
    static final DERObjectIdentifier    id_icao_ldsSecurityObject = new DERObjectIdentifier(id_icao_mrtd_security+".1");
}
