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

package com.dotcms.enterprise.license.bouncycastle.asn1.nist;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface NISTObjectIdentifiers
{
    //
    // NIST
    //     iso/itu(2) joint-assign(16) us(840) organization(1) gov(101) csor(3) 

    //
    // nistalgorithms(4)
    //
    static final String                 nistAlgorithm          = "2.16.840.1.101.3.4";

    static final DERObjectIdentifier    id_sha256               = new DERObjectIdentifier(nistAlgorithm + ".2.1");
    static final DERObjectIdentifier    id_sha384               = new DERObjectIdentifier(nistAlgorithm + ".2.2");
    static final DERObjectIdentifier    id_sha512               = new DERObjectIdentifier(nistAlgorithm + ".2.3");
    static final DERObjectIdentifier    id_sha224               = new DERObjectIdentifier(nistAlgorithm + ".2.4");
    
    static final String                 aes                     = nistAlgorithm + ".1";
    
    static final DERObjectIdentifier    id_aes128_ECB           = new DERObjectIdentifier(aes + ".1"); 
    static final DERObjectIdentifier    id_aes128_CBC           = new DERObjectIdentifier(aes + ".2");
    static final DERObjectIdentifier    id_aes128_OFB           = new DERObjectIdentifier(aes + ".3"); 
    static final DERObjectIdentifier    id_aes128_CFB           = new DERObjectIdentifier(aes + ".4"); 
    static final DERObjectIdentifier    id_aes128_wrap          = new DERObjectIdentifier(aes + ".5");
    static final DERObjectIdentifier    id_aes128_GCM           = new DERObjectIdentifier(aes + ".6");
    static final DERObjectIdentifier    id_aes128_CCM           = new DERObjectIdentifier(aes + ".7");
    
    static final DERObjectIdentifier    id_aes192_ECB           = new DERObjectIdentifier(aes + ".21"); 
    static final DERObjectIdentifier    id_aes192_CBC           = new DERObjectIdentifier(aes + ".22"); 
    static final DERObjectIdentifier    id_aes192_OFB           = new DERObjectIdentifier(aes + ".23"); 
    static final DERObjectIdentifier    id_aes192_CFB           = new DERObjectIdentifier(aes + ".24"); 
    static final DERObjectIdentifier    id_aes192_wrap          = new DERObjectIdentifier(aes + ".25");
    static final DERObjectIdentifier    id_aes192_GCM           = new DERObjectIdentifier(aes + ".26");
    static final DERObjectIdentifier    id_aes192_CCM           = new DERObjectIdentifier(aes + ".27");
    
    static final DERObjectIdentifier    id_aes256_ECB           = new DERObjectIdentifier(aes + ".41"); 
    static final DERObjectIdentifier    id_aes256_CBC           = new DERObjectIdentifier(aes + ".42");
    static final DERObjectIdentifier    id_aes256_OFB           = new DERObjectIdentifier(aes + ".43"); 
    static final DERObjectIdentifier    id_aes256_CFB           = new DERObjectIdentifier(aes + ".44"); 
    static final DERObjectIdentifier    id_aes256_wrap          = new DERObjectIdentifier(aes + ".45"); 
    static final DERObjectIdentifier    id_aes256_GCM           = new DERObjectIdentifier(aes + ".46");
    static final DERObjectIdentifier    id_aes256_CCM           = new DERObjectIdentifier(aes + ".47");

    //
    // signatures
    //
    static final DERObjectIdentifier    id_dsa_with_sha2        = new DERObjectIdentifier(nistAlgorithm + ".3"); 

    static final DERObjectIdentifier    dsa_with_sha224         = new DERObjectIdentifier(id_dsa_with_sha2 + ".1"); 
    static final DERObjectIdentifier    dsa_with_sha256         = new DERObjectIdentifier(id_dsa_with_sha2 + ".2");
    static final DERObjectIdentifier    dsa_with_sha384         = new DERObjectIdentifier(id_dsa_with_sha2 + ".3");
    static final DERObjectIdentifier    dsa_with_sha512         = new DERObjectIdentifier(id_dsa_with_sha2 + ".4"); 
}
