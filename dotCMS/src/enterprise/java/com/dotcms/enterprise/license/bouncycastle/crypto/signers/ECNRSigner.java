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

package com.dotcms.enterprise.license.bouncycastle.crypto.signers;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DSA;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.generators.ECKeyPairGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECKeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECAlgorithms;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECConstants;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * EC-NR as described in IEEE 1363-2000
 */
public class ECNRSigner
    implements DSA
{
    private boolean             forSigning;
    private ECKeyParameters     key;
    private SecureRandom        random;

    public void init(
        boolean          forSigning, 
        CipherParameters param) 
    {
        this.forSigning = forSigning;
        
        if (forSigning)
        {
            if (param instanceof ParametersWithRandom)
            {
                ParametersWithRandom    rParam = (ParametersWithRandom)param;

                this.random = rParam.getRandom();
                this.key = (ECPrivateKeyParameters)rParam.getParameters();
            }
            else
            {
                this.random = new SecureRandom();
                this.key = (ECPrivateKeyParameters)param;
            }
        }
        else
        {
            this.key = (ECPublicKeyParameters)param;
        }
    }

    // Section 7.2.5 ECSP-NR, pg 34
    /**
     * generate a signature for the given message using the key we were
     * initialised with.  Generally, the order of the curve should be at 
     * least as long as the hash of the message of interest, and with 
     * ECNR it *must* be at least as long.  
     *
     * @param digest  the digest to be signed.
     * @exception DataLengthException if the digest is longer than the key allows
     */
    public BigInteger[] generateSignature(
        byte[] digest)
    {
        if (! this.forSigning) 
        {
            throw new IllegalStateException("not initialised for signing");
        }
        
        BigInteger n = ((ECPrivateKeyParameters)this.key).getParameters().getN();
        int nBitLength = n.bitLength();
        
        BigInteger e = new BigInteger(1, digest);
        int eBitLength = e.bitLength();
        
        ECPrivateKeyParameters  privKey = (ECPrivateKeyParameters)key;
               
        if (eBitLength > nBitLength) 
        {
            throw new DataLengthException("input too large for ECNR key.");
        }

        BigInteger r = null;
        BigInteger s = null;

        AsymmetricCipherKeyPair tempPair;
        do // generate r
        {
            // generate another, but very temporary, key pair using 
            // the same EC parameters
            ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
            
            keyGen.init(new ECKeyGenerationParameters(privKey.getParameters(), this.random));
            
            tempPair = keyGen.generateKeyPair();

            //    BigInteger Vx = tempPair.getPublic().getW().getAffineX();
            ECPublicKeyParameters V = (ECPublicKeyParameters)tempPair.getPublic();        // get temp's public key
            BigInteger Vx = V.getQ().getX().toBigInteger();        // get the point's x coordinate
            
            r = Vx.add(e).mod(n);
        }
        while (r.equals(ECConstants.ZERO));

        // generate s
        BigInteger x = privKey.getD();                // private key value
        BigInteger u = ((ECPrivateKeyParameters)tempPair.getPrivate()).getD();    // temp's private key value
        s = u.subtract(r.multiply(x)).mod(n);

        BigInteger[]  res = new BigInteger[2];
        res[0] = r;
        res[1] = s;

        return res;
    }

    // Section 7.2.6 ECVP-NR, pg 35
    /**
     * return true if the value r and s represent a signature for the 
     * message passed in. Generally, the order of the curve should be at 
     * least as long as the hash of the message of interest, and with 
     * ECNR, it *must* be at least as long.  But just in case the signer
     * applied mod(n) to the longer digest, this implementation will
     * apply mod(n) during verification.
     *
     * @param digest  the digest to be verified.
     * @param r       the r value of the signature.
     * @param s       the s value of the signature.
     * @exception DataLengthException if the digest is longer than the key allows
     */
    public boolean verifySignature(
        byte[]      digest,
        BigInteger  r,
        BigInteger  s)
    {
        if (this.forSigning) 
        {
            throw new IllegalStateException("not initialised for verifying");
        }

        ECPublicKeyParameters pubKey = (ECPublicKeyParameters)key;
        BigInteger n = pubKey.getParameters().getN();
        int nBitLength = n.bitLength();
        
        BigInteger e = new BigInteger(1, digest);
        int eBitLength = e.bitLength();
        
        if (eBitLength > nBitLength) 
        {
            throw new DataLengthException("input too large for ECNR key.");
        }
        
        // r in the range [1,n-1]
        if (r.compareTo(ECConstants.ONE) < 0 || r.compareTo(n) >= 0) 
        {
            return false;
        }

        // s in the range [0,n-1]           NB: ECNR spec says 0
        if (s.compareTo(ECConstants.ZERO) < 0 || s.compareTo(n) >= 0) 
        {
            return false;
        }

        // compute P = sG + rW

        ECPoint G = pubKey.getParameters().getG();
        ECPoint W = pubKey.getQ();
        // calculate P using Bouncy math
        ECPoint P = ECAlgorithms.sumOfTwoMultiplies(G, s, W, r);

        BigInteger x = P.getX().toBigInteger();
        BigInteger t = r.subtract(x).mod(n);

        return t.equals(e);
    }
}
