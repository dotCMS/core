/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.crypto.encodings.PKCS1Encoding;
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.RSABlindedEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.signers.GenericSigner;

class TlsRSASigner
    extends GenericSigner
{
    TlsRSASigner()
    {
        super(new PKCS1Encoding(new RSABlindedEngine()), new CombinedHash());
    }
}
