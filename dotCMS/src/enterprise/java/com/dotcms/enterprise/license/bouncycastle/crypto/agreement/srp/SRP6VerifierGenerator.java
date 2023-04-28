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

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;

/**
 * Generates new SRP verifier for user
 */
public class SRP6VerifierGenerator
{
    protected BigInteger N;
    protected BigInteger g;
    protected Digest digest;

    public SRP6VerifierGenerator()
    {
    }

    /**
     * Initialises generator to create new verifiers
     * @param N The safe prime to use (see DHParametersGenerator)
     * @param g The group parameter to use (see DHParametersGenerator)
     * @param digest The digest to use. The same digest type will need to be used later for the actual authentication
     * attempt. Also note that the final session key size is dependent on the chosen digest.
     */
    public void init(BigInteger N, BigInteger g, Digest digest)
    {
        this.N = N;
        this.g = g;
        this.digest = digest;
    }

    /**
     * Creates a new SRP verifier
     * @param salt The salt to use, generally should be large and random
     * @param identity The user's identifying information (eg. username)
     * @param password The user's password
     * @return A new verifier for use in future SRP authentication
     */
    public BigInteger generateVerifier(byte[] salt, byte[] identity, byte[] password)
    {
        BigInteger x = SRP6Util.calculateX(digest, N, salt, identity, password);

        return g.modPow(x, N);
    }
}
