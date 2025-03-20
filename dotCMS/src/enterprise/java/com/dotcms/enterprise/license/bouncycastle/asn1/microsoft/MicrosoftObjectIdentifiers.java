/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.microsoft;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface MicrosoftObjectIdentifiers
{
    //
    // Microsoft
    //       iso(1) identified-organization(3) dod(6) internet(1) private(4) enterprise(1) microsoft(311)
    //
    static final DERObjectIdentifier    microsoft               = new DERObjectIdentifier("1.3.6.1.4.1.311");
    static final DERObjectIdentifier    microsoftCertTemplateV1 = new DERObjectIdentifier(microsoft + ".20.2");
    static final DERObjectIdentifier    microsoftCaVersion      = new DERObjectIdentifier(microsoft + ".21.1");
    static final DERObjectIdentifier    microsoftPrevCaCertHash = new DERObjectIdentifier(microsoft + ".21.2");
    static final DERObjectIdentifier    microsoftCertTemplateV2 = new DERObjectIdentifier(microsoft + ".21.7");
    static final DERObjectIdentifier    microsoftAppPolicies    = new DERObjectIdentifier(microsoft + ".21.10");
}
