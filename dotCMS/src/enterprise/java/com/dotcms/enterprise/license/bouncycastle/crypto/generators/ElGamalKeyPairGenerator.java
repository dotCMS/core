/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import java.math.BigInteger;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.KeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalPublicKeyParameters;

/**
 * a ElGamal key pair generator.
 * <p>
 * This generates keys consistent for use with ElGamal as described in
 * page 164 of "Handbook of Applied Cryptography".
 */
public class ElGamalKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private ElGamalKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (ElGamalKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        DHKeyGeneratorHelper helper = DHKeyGeneratorHelper.INSTANCE;
        ElGamalParameters egp = param.getParameters();
        DHParameters dhp = new DHParameters(egp.getP(), egp.getG(), null, egp.getL());  

        BigInteger x = helper.calculatePrivate(dhp, param.getRandom()); 
        BigInteger y = helper.calculatePublic(dhp, x);

        return new AsymmetricCipherKeyPair(
            new ElGamalPublicKeyParameters(y, egp),
            new ElGamalPrivateKeyParameters(x, egp));
    }
}
