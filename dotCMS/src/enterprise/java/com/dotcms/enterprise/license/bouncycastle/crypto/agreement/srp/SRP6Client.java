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

package com.dotcms.enterprise.license.bouncycastle.crypto.agreement.srp;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.CryptoException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;

/**
 * Implements the client side SRP-6a protocol. Note that this class is stateful, and therefore NOT threadsafe.
 * This implementation of SRP is based on the optimized message sequence put forth by Thomas Wu in the paper
 * "SRP-6: Improvements and Refinements to the Secure Remote Password Protocol, 2002"
 */
public class SRP6Client
{
    protected BigInteger N;
    protected BigInteger g;

    protected BigInteger a;
    protected BigInteger A;

    protected BigInteger B;

    protected BigInteger x;
    protected BigInteger u;
    protected BigInteger S;

    protected Digest digest;
    protected SecureRandom random;

    public SRP6Client()
    {
    }

    /**
     * Initialises the client to begin new authentication attempt
     * @param N The safe prime associated with the client's verifier
     * @param g The group parameter associated with the client's verifier
     * @param digest The digest algorithm associated with the client's verifier
     * @param random For key generation
     */
    public void init(BigInteger N, BigInteger g, Digest digest, SecureRandom random)
    {
        this.N = N;
        this.g = g;
        this.digest = digest;
        this.random = random;
    }

    /**
     * Generates client's credentials given the client's salt, identity and password
     * @param salt The salt used in the client's verifier.
     * @param identity The user's identity (eg. username)
     * @param password The user's password
     * @return Client's public value to send to server
     */
    public BigInteger generateClientCredentials(byte[] salt, byte[] identity, byte[] password)
    {
        this.x = SRP6Util.calculateX(digest, N, salt, identity, password);
        this.a = selectPrivateValue();
        this.A = g.modPow(a, N);

        return A;
    }

    /**
     * Generates client's verification message given the server's credentials
     * @param serverB The server's credentials
     * @return Client's verification message for the server
     * @throws CryptoException If server's credentials are invalid
     */
    public BigInteger calculateSecret(BigInteger serverB) throws CryptoException
    {
        this.B = SRP6Util.validatePublicValue(N, serverB);
        this.u = SRP6Util.calculateU(digest, N, A, B);
        this.S = calculateS();

        return S;
    }

    protected BigInteger selectPrivateValue()
    {
        return SRP6Util.generatePrivateValue(digest, N, g, random);        
    }

    private BigInteger calculateS()
    {
        BigInteger k = SRP6Util.calculateK(digest, N, g);
        BigInteger exp = u.multiply(x).add(a);
        BigInteger tmp = g.modPow(x, N).multiply(k).mod(N);
        return B.subtract(tmp).mod(N).modPow(exp, N);
    }
}
