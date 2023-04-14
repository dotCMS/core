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

package com.dotcms.enterprise.license.bouncycastle.asn1.bc;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface BCObjectIdentifiers
{
    /**
     *  iso.org.dod.internet.private.enterprise.legion-of-the-bouncy-castle
     *
     *  1.3.6.1.4.1.22554
     */
    public static final DERObjectIdentifier bc = new DERObjectIdentifier("1.3.6.1.4.1.22554");

    /**
     * pbe(1) algorithms
     */
    public static final DERObjectIdentifier bc_pbe = new DERObjectIdentifier(bc.getId() + ".1");

    /**
     * SHA-1(1)
     */
    public static final DERObjectIdentifier bc_pbe_sha1 = new DERObjectIdentifier(bc_pbe.getId() + ".1");

    /**
     * SHA-2(2) . (SHA-256(1)|SHA-384(2)|SHA-512(3)|SHA-224(4))
     */
    public static final DERObjectIdentifier bc_pbe_sha256 = new DERObjectIdentifier(bc_pbe.getId() + ".2.1");
    public static final DERObjectIdentifier bc_pbe_sha384 = new DERObjectIdentifier(bc_pbe.getId() + ".2.2");
    public static final DERObjectIdentifier bc_pbe_sha512 = new DERObjectIdentifier(bc_pbe.getId() + ".2.3");
    public static final DERObjectIdentifier bc_pbe_sha224 = new DERObjectIdentifier(bc_pbe.getId() + ".2.4");

    /**
     * PKCS-5(1)|PKCS-12(2)
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs5 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".2");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs5 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".2");

    /**
     * AES(1) . (CBC-128(2)|CBC-192(22)|CBC-256(42))
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.42");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.42");
}
