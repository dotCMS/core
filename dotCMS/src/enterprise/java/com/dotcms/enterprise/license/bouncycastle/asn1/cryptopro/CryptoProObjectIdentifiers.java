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

package com.dotcms.enterprise.license.bouncycastle.asn1.cryptopro;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface CryptoProObjectIdentifiers
{
    // GOST Algorithms OBJECT IDENTIFIERS :
    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2)}
    static final String                 GOST_id              = "1.2.643.2.2";

    static final DERObjectIdentifier    gostR3411          = new DERObjectIdentifier(GOST_id+".9");
    
    static final DERObjectIdentifier    gostR28147_cbc     = new DERObjectIdentifier(GOST_id+".21");

    static final DERObjectIdentifier    gostR3410_94       = new DERObjectIdentifier(GOST_id+".20");
    static final DERObjectIdentifier    gostR3410_2001     = new DERObjectIdentifier(GOST_id+".19");
    static final DERObjectIdentifier    gostR3411_94_with_gostR3410_94   = new DERObjectIdentifier(GOST_id+".4");
    static final DERObjectIdentifier    gostR3411_94_with_gostR3410_2001 = new DERObjectIdentifier(GOST_id+".3");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) hashes(30) }
    static final DERObjectIdentifier    gostR3411_94_CryptoProParamSet = new DERObjectIdentifier(GOST_id+".30.1");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) signs(32) }
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_A     = new DERObjectIdentifier(GOST_id+".32.2");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_B     = new DERObjectIdentifier(GOST_id+".32.3");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_C     = new DERObjectIdentifier(GOST_id+".32.4");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_D     = new DERObjectIdentifier(GOST_id+".32.5");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) exchanges(33) }
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchA  = new DERObjectIdentifier(GOST_id+".33.1");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchB  = new DERObjectIdentifier(GOST_id+".33.2");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchC  = new DERObjectIdentifier(GOST_id+".33.3");

    //{ iso(1) member-body(2)ru(643) rans(2) cryptopro(2) ecc-signs(35) }
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_A = new DERObjectIdentifier(GOST_id+".35.1");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_B = new DERObjectIdentifier(GOST_id+".35.2");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_C = new DERObjectIdentifier(GOST_id+".35.3");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) ecc-exchanges(36) }
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_XchA  = new DERObjectIdentifier(GOST_id+".36.0");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_XchB  = new DERObjectIdentifier(GOST_id+".36.1");
    
    static final DERObjectIdentifier    gost_ElSgDH3410_default    = new DERObjectIdentifier(GOST_id+".36.0");
    static final DERObjectIdentifier    gost_ElSgDH3410_1          = new DERObjectIdentifier(GOST_id+".36.1");
}
