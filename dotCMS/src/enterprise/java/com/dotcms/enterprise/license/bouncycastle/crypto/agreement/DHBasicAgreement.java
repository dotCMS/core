/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.agreement;

import java.math.BigInteger;

import com.dotcms.enterprise.license.bouncycastle.crypto.BasicAgreement;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;

/**
 * a Diffie-Hellman key agreement class.
 * <p>
 * note: This is only the basic algorithm, it doesn't take advantage of
 * long term public keys if they are available. See the DHAgreement class
 * for a "better" implementation.
 */
public class DHBasicAgreement
    implements BasicAgreement
{
    private DHPrivateKeyParameters  key;
    private DHParameters            dhParams;

    public void init(
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            kParam = (AsymmetricKeyParameter)param;
        }

        if (!(kParam instanceof DHPrivateKeyParameters))
        {
            throw new IllegalArgumentException("DHEngine expects DHPrivateKeyParameters");
        }

        this.key = (DHPrivateKeyParameters)kParam;
        this.dhParams = key.getParameters();
    }

    /**
     * given a short term public key from a given party calculate the next
     * message in the agreement sequence. 
     */
    public BigInteger calculateAgreement(
        CipherParameters   pubKey)
    {
        DHPublicKeyParameters   pub = (DHPublicKeyParameters)pubKey;

        if (!pub.getParameters().equals(dhParams))
        {
            throw new IllegalArgumentException("Diffie-Hellman public key has wrong parameters.");
        }

        return pub.getY().modPow(key.getX(), dhParams.getP());
    }
}
