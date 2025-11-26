/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;

public class MQVPrivateParameters
    implements CipherParameters
{
    private ECPrivateKeyParameters staticPrivateKey;
    private ECPrivateKeyParameters ephemeralPrivateKey;
    private ECPublicKeyParameters ephemeralPublicKey;

    public MQVPrivateParameters(
        ECPrivateKeyParameters  staticPrivateKey,
        ECPrivateKeyParameters  ephemeralPrivateKey)
    {
        this(staticPrivateKey, ephemeralPrivateKey, null);
    }

    public MQVPrivateParameters(
        ECPrivateKeyParameters  staticPrivateKey,
        ECPrivateKeyParameters  ephemeralPrivateKey,
        ECPublicKeyParameters   ephemeralPublicKey)
    {
        this.staticPrivateKey = staticPrivateKey;
        this.ephemeralPrivateKey = ephemeralPrivateKey;
        this.ephemeralPublicKey = ephemeralPublicKey;
    }

    public ECPrivateKeyParameters getStaticPrivateKey()
    {
        return staticPrivateKey;
    }

    public ECPrivateKeyParameters getEphemeralPrivateKey()
    {
        return ephemeralPrivateKey;
    }

    public ECPublicKeyParameters getEphemeralPublicKey()
    {
        return ephemeralPublicKey;
    }
}
