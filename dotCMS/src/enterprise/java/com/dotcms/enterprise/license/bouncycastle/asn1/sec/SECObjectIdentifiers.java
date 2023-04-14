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

package com.dotcms.enterprise.license.bouncycastle.asn1.sec;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X9ObjectIdentifiers;

public interface SECObjectIdentifiers
{
    /**
     *  ellipticCurve OBJECT IDENTIFIER ::= {
     *        iso(1) identified-organization(3) certicom(132) curve(0)
     *  }
     */
    static final DERObjectIdentifier ellipticCurve = new DERObjectIdentifier("1.3.132.0");

    static final DERObjectIdentifier sect163k1 = new DERObjectIdentifier(ellipticCurve + ".1");
    static final DERObjectIdentifier sect163r1 = new DERObjectIdentifier(ellipticCurve + ".2");
    static final DERObjectIdentifier sect239k1 = new DERObjectIdentifier(ellipticCurve + ".3");
    static final DERObjectIdentifier sect113r1 = new DERObjectIdentifier(ellipticCurve + ".4");
    static final DERObjectIdentifier sect113r2 = new DERObjectIdentifier(ellipticCurve + ".5");
    static final DERObjectIdentifier secp112r1 = new DERObjectIdentifier(ellipticCurve + ".6");
    static final DERObjectIdentifier secp112r2 = new DERObjectIdentifier(ellipticCurve + ".7");
    static final DERObjectIdentifier secp160r1 = new DERObjectIdentifier(ellipticCurve + ".8");
    static final DERObjectIdentifier secp160k1 = new DERObjectIdentifier(ellipticCurve + ".9");
    static final DERObjectIdentifier secp256k1 = new DERObjectIdentifier(ellipticCurve + ".10");
    static final DERObjectIdentifier sect163r2 = new DERObjectIdentifier(ellipticCurve + ".15");
    static final DERObjectIdentifier sect283k1 = new DERObjectIdentifier(ellipticCurve + ".16");
    static final DERObjectIdentifier sect283r1 = new DERObjectIdentifier(ellipticCurve + ".17");
    static final DERObjectIdentifier sect131r1 = new DERObjectIdentifier(ellipticCurve + ".22");
    static final DERObjectIdentifier sect131r2 = new DERObjectIdentifier(ellipticCurve + ".23");
    static final DERObjectIdentifier sect193r1 = new DERObjectIdentifier(ellipticCurve + ".24");
    static final DERObjectIdentifier sect193r2 = new DERObjectIdentifier(ellipticCurve + ".25");
    static final DERObjectIdentifier sect233k1 = new DERObjectIdentifier(ellipticCurve + ".26");
    static final DERObjectIdentifier sect233r1 = new DERObjectIdentifier(ellipticCurve + ".27");
    static final DERObjectIdentifier secp128r1 = new DERObjectIdentifier(ellipticCurve + ".28");
    static final DERObjectIdentifier secp128r2 = new DERObjectIdentifier(ellipticCurve + ".29");
    static final DERObjectIdentifier secp160r2 = new DERObjectIdentifier(ellipticCurve + ".30");
    static final DERObjectIdentifier secp192k1 = new DERObjectIdentifier(ellipticCurve + ".31");
    static final DERObjectIdentifier secp224k1 = new DERObjectIdentifier(ellipticCurve + ".32");
    static final DERObjectIdentifier secp224r1 = new DERObjectIdentifier(ellipticCurve + ".33");
    static final DERObjectIdentifier secp384r1 = new DERObjectIdentifier(ellipticCurve + ".34");
    static final DERObjectIdentifier secp521r1 = new DERObjectIdentifier(ellipticCurve + ".35");
    static final DERObjectIdentifier sect409k1 = new DERObjectIdentifier(ellipticCurve + ".36");
    static final DERObjectIdentifier sect409r1 = new DERObjectIdentifier(ellipticCurve + ".37");
    static final DERObjectIdentifier sect571k1 = new DERObjectIdentifier(ellipticCurve + ".38");
    static final DERObjectIdentifier sect571r1 = new DERObjectIdentifier(ellipticCurve + ".39");

    static final DERObjectIdentifier secp192r1 = X9ObjectIdentifiers.prime192v1;
    static final DERObjectIdentifier secp256r1 = X9ObjectIdentifiers.prime256v1;

}
