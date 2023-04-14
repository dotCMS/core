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

package com.dotcms.enterprise.license.bouncycastle.asn1.eac;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface EACObjectIdentifiers
{
    // bsi-de OBJECT IDENTIFIER ::= {
    //         itu-t(0) identified-organization(4) etsi(0)
    //         reserved(127) etsi-identified-organization(0) 7
    //     }
    static final DERObjectIdentifier    bsi_de      = new DERObjectIdentifier("0.4.0.127.0.7");

    // id-PK OBJECT IDENTIFIER ::= {
    //         bsi-de protocols(2) smartcard(2) 1
    //     }
    static final DERObjectIdentifier    id_PK = new DERObjectIdentifier(bsi_de + ".2.2.1");

    static final DERObjectIdentifier    id_PK_DH = new DERObjectIdentifier(id_PK + ".1");
    static final DERObjectIdentifier    id_PK_ECDH = new DERObjectIdentifier(id_PK + ".2");

    // id-CA OBJECT IDENTIFIER ::= {
    //         bsi-de protocols(2) smartcard(2) 3
    //     }
    static final DERObjectIdentifier    id_CA = new DERObjectIdentifier(bsi_de + ".2.2.3");
    static final DERObjectIdentifier    id_CA_DH = new DERObjectIdentifier(id_CA + ".1");
    static final DERObjectIdentifier    id_CA_DH_3DES_CBC_CBC = new DERObjectIdentifier(id_CA_DH + ".1");
    static final DERObjectIdentifier    id_CA_ECDH = new DERObjectIdentifier(id_CA + ".2");
    static final DERObjectIdentifier    id_CA_ECDH_3DES_CBC_CBC = new DERObjectIdentifier(id_CA_ECDH + ".1");

    //
    // id-TA OBJECT IDENTIFIER ::= {
    //     bsi-de protocols(2) smartcard(2) 2
    // }
    static final DERObjectIdentifier    id_TA = new DERObjectIdentifier(bsi_de + ".2.2.2");

    static final DERObjectIdentifier    id_TA_RSA = new DERObjectIdentifier(id_TA + ".1");
    static final DERObjectIdentifier    id_TA_RSA_v1_5_SHA_1 = new DERObjectIdentifier(id_TA_RSA + ".1");
    static final DERObjectIdentifier    id_TA_RSA_v1_5_SHA_256 = new DERObjectIdentifier(id_TA_RSA + ".2");
    static final DERObjectIdentifier    id_TA_RSA_PSS_SHA_1 = new DERObjectIdentifier(id_TA_RSA + ".3");
    static final DERObjectIdentifier    id_TA_RSA_PSS_SHA_256 = new DERObjectIdentifier(id_TA_RSA + ".4");
    static final DERObjectIdentifier    id_TA_ECDSA = new DERObjectIdentifier(id_TA + ".2");
    static final DERObjectIdentifier    id_TA_ECDSA_SHA_1 = new DERObjectIdentifier(id_TA_ECDSA + ".1");
    static final DERObjectIdentifier    id_TA_ECDSA_SHA_224 = new DERObjectIdentifier(id_TA_ECDSA + ".2");
    static final DERObjectIdentifier    id_TA_ECDSA_SHA_256 = new DERObjectIdentifier(id_TA_ECDSA + ".3");

    static final DERObjectIdentifier    id_TA_ECDSA_SHA_384 = new DERObjectIdentifier(id_TA_ECDSA + ".4");
    static final DERObjectIdentifier    id_TA_ECDSA_SHA_512 = new DERObjectIdentifier(id_TA_ECDSA + ".5");

    /**
     * id-EAC-ePassport OBJECT IDENTIFIER ::= {
     * bsi-de applications(3) mrtd(1) roles(2) 1}
     */
    static final DERObjectIdentifier id_EAC_ePassport = new DERObjectIdentifier(bsi_de + ".3.1.2.1");

}
