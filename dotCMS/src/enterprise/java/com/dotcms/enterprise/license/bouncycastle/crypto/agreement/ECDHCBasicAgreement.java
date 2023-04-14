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
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

/**
 * P1363 7.2.2 ECSVDP-DHC
 *
 * ECSVDP-DHC is Elliptic Curve Secret Value Derivation Primitive,
 * Diffie-Hellman version with cofactor multiplication. It is based on
 * the work of [DH76], [Mil86], [Kob87], [LMQ98] and [Kal98a]. This
 * primitive derives a shared secret value from one party's private key
 * and another party's public key, where both have the same set of EC
 * domain parameters. If two parties correctly execute this primitive,
 * they will produce the same output. This primitive can be invoked by a
 * scheme to derive a shared secret key; specifically, it may be used
 * with the schemes ECKAS-DH1 and DL/ECKAS-DH2. It does not assume the
 * validity of the input public key (see also Section 7.2.1).
 * <p>
 * Note: As stated P1363 compatibility mode with ECDH can be preset, and
 * in this case the implementation doesn't have a ECDH compatibility mode
 * (if you want that just use ECDHBasicAgreement and note they both implement
 * BasicAgreement!).
 */
public class ECDHCBasicAgreement
    implements BasicAgreement
{
    ECPrivateKeyParameters key;

    public void init(
        CipherParameters key)
    {
        this.key = (ECPrivateKeyParameters)key;
    }

    public BigInteger calculateAgreement(
        CipherParameters pubKey)
    {
        ECPublicKeyParameters   pub = (ECPublicKeyParameters)pubKey;
        ECDomainParameters      params = pub.getParameters();
        ECPoint P = pub.getQ().multiply(params.getH().multiply(key.getD()));

        // if (p.isInfinity()) throw new RuntimeException("Invalid public key");

        return P.getX().toBigInteger();
    }
}
