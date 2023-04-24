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

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSABlindingParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;

import java.math.BigInteger;

/**
 * This does your basic RSA Chaum's blinding and unblinding as outlined in
 * "Handbook of Applied Cryptography", page 475. You need to use this if you are
 * trying to get another party to generate signatures without them being aware
 * of the message they are signing.
 */
public class RSABlindingEngine
    implements AsymmetricBlockCipher
{
    private RSACoreEngine core = new RSACoreEngine();

    private RSAKeyParameters key;
    private BigInteger blindingFactor;

    private boolean forEncryption;

    /**
     * Initialise the blinding engine.
     *
     * @param forEncryption true if we are encrypting (blinding), false otherwise.
     * @param param         the necessary RSA key parameters.
     */
    public void init(
        boolean forEncryption,
        CipherParameters param)
    {
        RSABlindingParameters p;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;

            p = (RSABlindingParameters)rParam.getParameters();
        }
        else
        {
            p = (RSABlindingParameters)param;
        }

        core.init(forEncryption, p.getPublicKey());

        this.forEncryption = forEncryption;
        this.key = p.getPublicKey();
        this.blindingFactor = p.getBlindingFactor();
    }

    /**
     * Return the maximum size for an input block to this engine.
     * For RSA this is always one byte less than the key size on
     * encryption, and the same length as the key size on decryption.
     *
     * @return maximum size for an input block.
     */
    public int getInputBlockSize()
    {
        return core.getInputBlockSize();
    }

    /**
     * Return the maximum size for an output block to this engine.
     * For RSA this is always one byte less than the key size on
     * decryption, and the same length as the key size on encryption.
     *
     * @return maximum size for an output block.
     */
    public int getOutputBlockSize()
    {
        return core.getOutputBlockSize();
    }

    /**
     * Process a single block using the RSA blinding algorithm.
     *
     * @param in    the input array.
     * @param inOff the offset into the input buffer where the data starts.
     * @param inLen the length of the data to be processed.
     * @return the result of the RSA process.
     * @throws DataLengthException the input block is too large.
     */
    public byte[] processBlock(
        byte[] in,
        int inOff,
        int inLen)
    {
        BigInteger msg = core.convertInput(in, inOff, inLen);

        if (forEncryption)
        {
            msg = blindMessage(msg);
        }
        else
        {
            msg = unblindMessage(msg);
        }

        return core.convertOutput(msg);
    }

    /*
     * Blind message with the blind factor.
     */
    private BigInteger blindMessage(
        BigInteger msg)
    {
        BigInteger blindMsg = blindingFactor;
        blindMsg = msg.multiply(blindMsg.modPow(key.getExponent(), key.getModulus()));
        blindMsg = blindMsg.mod(key.getModulus());

        return blindMsg;
    }

    /*
     * Unblind the message blinded with the blind factor.
     */
    private BigInteger unblindMessage(
        BigInteger blindedMsg)
    {
        BigInteger m = key.getModulus();
        BigInteger msg = blindedMsg;
        BigInteger blindFactorInverse = blindingFactor.modInverse(m);
        msg = msg.multiply(blindFactorInverse);
        msg = msg.mod(m);

        return msg;
    }
}
