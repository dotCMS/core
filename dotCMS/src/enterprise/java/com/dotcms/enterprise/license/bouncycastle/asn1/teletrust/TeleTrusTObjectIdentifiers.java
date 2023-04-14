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

package com.dotcms.enterprise.license.bouncycastle.asn1.teletrust;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface TeleTrusTObjectIdentifiers
{
    static final String teleTrusTAlgorithm = "1.3.36.3";

    static final DERObjectIdentifier    ripemd160           = new DERObjectIdentifier(teleTrusTAlgorithm + ".2.1");
    static final DERObjectIdentifier    ripemd128           = new DERObjectIdentifier(teleTrusTAlgorithm + ".2.2");
    static final DERObjectIdentifier    ripemd256           = new DERObjectIdentifier(teleTrusTAlgorithm + ".2.3");

    static final String teleTrusTRSAsignatureAlgorithm = teleTrusTAlgorithm + ".3.1";

    static final DERObjectIdentifier    rsaSignatureWithripemd160           = new DERObjectIdentifier(teleTrusTRSAsignatureAlgorithm + ".2");
    static final DERObjectIdentifier    rsaSignatureWithripemd128           = new DERObjectIdentifier(teleTrusTRSAsignatureAlgorithm + ".3");
    static final DERObjectIdentifier    rsaSignatureWithripemd256           = new DERObjectIdentifier(teleTrusTRSAsignatureAlgorithm + ".4");

    static final DERObjectIdentifier    ecSign = new DERObjectIdentifier(teleTrusTAlgorithm + ".3.2");

    static final DERObjectIdentifier    ecSignWithSha1  = new DERObjectIdentifier(ecSign + ".1");
    static final DERObjectIdentifier    ecSignWithRipemd160  = new DERObjectIdentifier(ecSign + ".2");

    static final DERObjectIdentifier ecc_brainpool = new DERObjectIdentifier(teleTrusTAlgorithm + ".3.2.8");
    static final DERObjectIdentifier ellipticCurve = new DERObjectIdentifier(ecc_brainpool + ".1");
    static final DERObjectIdentifier versionOne = new DERObjectIdentifier(ellipticCurve + ".1");    

    static final DERObjectIdentifier brainpoolP160r1 = new DERObjectIdentifier(versionOne + ".1");
    static final DERObjectIdentifier brainpoolP160t1 = new DERObjectIdentifier(versionOne + ".2");
    static final DERObjectIdentifier brainpoolP192r1 = new DERObjectIdentifier(versionOne + ".3");
    static final DERObjectIdentifier brainpoolP192t1 = new DERObjectIdentifier(versionOne + ".4");
    static final DERObjectIdentifier brainpoolP224r1 = new DERObjectIdentifier(versionOne + ".5");
    static final DERObjectIdentifier brainpoolP224t1 = new DERObjectIdentifier(versionOne + ".6");
    static final DERObjectIdentifier brainpoolP256r1 = new DERObjectIdentifier(versionOne + ".7");
    static final DERObjectIdentifier brainpoolP256t1 = new DERObjectIdentifier(versionOne + ".8");
    static final DERObjectIdentifier brainpoolP320r1 = new DERObjectIdentifier(versionOne + ".9");
    static final DERObjectIdentifier brainpoolP320t1 = new DERObjectIdentifier(versionOne+".10");
    static final DERObjectIdentifier brainpoolP384r1 = new DERObjectIdentifier(versionOne+".11");
    static final DERObjectIdentifier brainpoolP384t1 = new DERObjectIdentifier(versionOne+".12");
    static final DERObjectIdentifier brainpoolP512r1 = new DERObjectIdentifier(versionOne+".13");
    static final DERObjectIdentifier brainpoolP512t1 = new DERObjectIdentifier(versionOne+".14");
}
