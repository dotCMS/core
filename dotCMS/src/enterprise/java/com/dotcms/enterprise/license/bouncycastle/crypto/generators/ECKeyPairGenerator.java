package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.KeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECDomainParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECKeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECConstants;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

public class ECKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator, ECConstants
{
    ECDomainParameters  params;
    SecureRandom        random;

    public void init(
        KeyGenerationParameters param)
    {
        ECKeyGenerationParameters  ecP = (ECKeyGenerationParameters)param;

        this.random = ecP.getRandom();
        this.params = ecP.getDomainParameters();
    }

    /**
     * Given the domain parameters this routine generates an EC key
     * pair in accordance with X9.62 section 5.2.1 pages 26, 27.
     */
    public AsymmetricCipherKeyPair generateKeyPair()
    {
        BigInteger n = params.getN();
        int        nBitLength = n.bitLength();
        BigInteger d;

        do
        {
            d = new BigInteger(nBitLength, random);
        }
        while (d.equals(ZERO)  || (d.compareTo(n) >= 0));

        ECPoint Q = params.getG().multiply(d);

        return new AsymmetricCipherKeyPair(
            new ECPublicKeyParameters(Q, params),
            new ECPrivateKeyParameters(d, params));
    }
}
