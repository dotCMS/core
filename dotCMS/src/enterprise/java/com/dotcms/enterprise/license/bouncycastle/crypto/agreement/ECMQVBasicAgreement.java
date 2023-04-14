/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.agreement;

import java.math.BigInteger;

import com.dotcms.enterprise.license.bouncycastle.crypto.BasicAgreement;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECDomainParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.MQVPrivateParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.MQVPublicParameters;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECAlgorithms;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECConstants;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

public class ECMQVBasicAgreement
    implements BasicAgreement
{
    MQVPrivateParameters privParams;

    public void init(
        CipherParameters key)
    {
        this.privParams = (MQVPrivateParameters)key;
    }

    public BigInteger calculateAgreement(CipherParameters pubKey)
    {
        MQVPublicParameters pubParams = (MQVPublicParameters)pubKey;

        ECPrivateKeyParameters staticPrivateKey = privParams.getStaticPrivateKey();

        ECPoint agreement = calculateMqvAgreement(staticPrivateKey.getParameters(), staticPrivateKey,
            privParams.getEphemeralPrivateKey(), privParams.getEphemeralPublicKey(),
            pubParams.getStaticPublicKey(), pubParams.getEphemeralPublicKey());

        return agreement.getX().toBigInteger();
    }

    // The ECMQV Primitive as described in SEC-1, 3.4
    private ECPoint calculateMqvAgreement(
        ECDomainParameters      parameters,
        ECPrivateKeyParameters  d1U,
        ECPrivateKeyParameters  d2U,
        ECPublicKeyParameters   Q2U,
        ECPublicKeyParameters   Q1V,
        ECPublicKeyParameters   Q2V)
    {
        BigInteger n = parameters.getN();
        int e = (n.bitLength() + 1) / 2;
        BigInteger powE = ECConstants.ONE.shiftLeft(e);

        // The Q2U public key is optional
        ECPoint q;
        if (Q2U == null)
        {
            q = parameters.getG().multiply(d2U.getD());
        }
        else
        {
            q = Q2U.getQ();
        }

        BigInteger x = q.getX().toBigInteger();
        BigInteger xBar = x.mod(powE);
        BigInteger Q2UBar = xBar.setBit(e);
        BigInteger s = d1U.getD().multiply(Q2UBar).mod(n).add(d2U.getD()).mod(n);

        BigInteger xPrime = Q2V.getQ().getX().toBigInteger();
        BigInteger xPrimeBar = xPrime.mod(powE);
        BigInteger Q2VBar = xPrimeBar.setBit(e);

        BigInteger hs = parameters.getH().multiply(s).mod(n);

//        ECPoint p = Q1V.getQ().multiply(Q2VBar).add(Q2V.getQ()).multiply(hs);
        ECPoint p = ECAlgorithms.sumOfTwoMultiplies(
            Q1V.getQ(), Q2VBar.multiply(hs).mod(n), Q2V.getQ(), hs);

        if (p.isInfinity())
        {
            throw new IllegalStateException("Infinity is not a valid agreement value for MQV");
        }

        return p;
    }
}
