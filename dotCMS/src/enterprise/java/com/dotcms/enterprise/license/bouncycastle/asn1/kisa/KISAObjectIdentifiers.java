/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.kisa;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface KISAObjectIdentifiers
{
    public static final DERObjectIdentifier id_seedCBC = new DERObjectIdentifier("1.2.410.200004.1.4");
    public static final DERObjectIdentifier id_npki_app_cmsSeed_wrap = new DERObjectIdentifier("1.2.410.200004.7.1.1.1");
}
