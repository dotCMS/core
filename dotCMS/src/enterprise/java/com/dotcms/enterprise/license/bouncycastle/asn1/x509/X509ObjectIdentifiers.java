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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface X509ObjectIdentifiers
{
    //
    // base id
    //
    static final String                 id                      = "2.5.4";

    static final DERObjectIdentifier    commonName              = new DERObjectIdentifier(id + ".3");
    static final DERObjectIdentifier    countryName             = new DERObjectIdentifier(id + ".6");
    static final DERObjectIdentifier    localityName            = new DERObjectIdentifier(id + ".7");
    static final DERObjectIdentifier    stateOrProvinceName     = new DERObjectIdentifier(id + ".8");
    static final DERObjectIdentifier    organization            = new DERObjectIdentifier(id + ".10");
    static final DERObjectIdentifier    organizationalUnitName  = new DERObjectIdentifier(id + ".11");

    static final DERObjectIdentifier    id_at_telephoneNumber   = new DERObjectIdentifier("2.5.4.20");
    static final DERObjectIdentifier    id_at_name              = new DERObjectIdentifier(id + ".41");

    // id-SHA1 OBJECT IDENTIFIER ::=    
    //   {iso(1) identified-organization(3) oiw(14) secsig(3) algorithms(2) 26 }    //
    static final DERObjectIdentifier    id_SHA1                 = new DERObjectIdentifier("1.3.14.3.2.26");

    //
    // ripemd160 OBJECT IDENTIFIER ::=
    //      {iso(1) identified-organization(3) TeleTrust(36) algorithm(3) hashAlgorithm(2) RIPEMD-160(1)}
    //
    static final DERObjectIdentifier    ripemd160               = new DERObjectIdentifier("1.3.36.3.2.1");

    //
    // ripemd160WithRSAEncryption OBJECT IDENTIFIER ::=
    //      {iso(1) identified-organization(3) TeleTrust(36) algorithm(3) signatureAlgorithm(3) rsaSignature(1) rsaSignatureWithripemd160(2) }
    //
    static final DERObjectIdentifier    ripemd160WithRSAEncryption = new DERObjectIdentifier("1.3.36.3.3.1.2");


    static final DERObjectIdentifier    id_ea_rsa = new DERObjectIdentifier("2.5.8.1.1");
    
    // id-pkix
    static final DERObjectIdentifier id_pkix = new DERObjectIdentifier("1.3.6.1.5.5.7");

    //
    // private internet extensions
    //
    static final DERObjectIdentifier  id_pe = new DERObjectIdentifier(id_pkix + ".1");

    //
    // authority information access
    //
    static final DERObjectIdentifier  id_ad = new DERObjectIdentifier(id_pkix + ".48");
    static final DERObjectIdentifier  id_ad_caIssuers = new DERObjectIdentifier(id_ad + ".2");
    static final DERObjectIdentifier  id_ad_ocsp = new DERObjectIdentifier(id_ad + ".1");

    //
    //    OID for ocsp and crl uri in AuthorityInformationAccess extension
    //
    static final DERObjectIdentifier ocspAccessMethod = id_ad_ocsp;
    static final DERObjectIdentifier crlAccessMethod = id_ad_caIssuers;
}

