/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.ocsp;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface OCSPObjectIdentifiers
{
    public static final String pkix_ocsp = "1.3.6.1.5.5.7.48.1";

    public static final DERObjectIdentifier id_pkix_ocsp = new DERObjectIdentifier(pkix_ocsp);
    public static final DERObjectIdentifier id_pkix_ocsp_basic = new DERObjectIdentifier(pkix_ocsp + ".1");
    
    //
    // extensions
    //
    public static final DERObjectIdentifier id_pkix_ocsp_nonce = new DERObjectIdentifier(pkix_ocsp + ".2");
    public static final DERObjectIdentifier id_pkix_ocsp_crl = new DERObjectIdentifier(pkix_ocsp + ".3");
    
    public static final DERObjectIdentifier id_pkix_ocsp_response = new DERObjectIdentifier(pkix_ocsp + ".4");
    public static final DERObjectIdentifier id_pkix_ocsp_nocheck = new DERObjectIdentifier(pkix_ocsp + ".5");
    public static final DERObjectIdentifier id_pkix_ocsp_archive_cutoff = new DERObjectIdentifier(pkix_ocsp + ".6");
    public static final DERObjectIdentifier id_pkix_ocsp_service_locator = new DERObjectIdentifier(pkix_ocsp + ".7");
}
