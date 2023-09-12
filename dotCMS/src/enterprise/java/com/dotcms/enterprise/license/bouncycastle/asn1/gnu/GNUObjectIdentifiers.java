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

package com.dotcms.enterprise.license.bouncycastle.asn1.gnu;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface GNUObjectIdentifiers
{
    public static final DERObjectIdentifier GNU = new DERObjectIdentifier("1.3.6.1.4.1.11591.1"); // GNU Radius
    public static final DERObjectIdentifier GnuPG = new DERObjectIdentifier("1.3.6.1.4.1.11591.2"); // GnuPG (Ägypten)
    public static final DERObjectIdentifier notation = new DERObjectIdentifier("1.3.6.1.4.1.11591.2.1"); // notation
    public static final DERObjectIdentifier pkaAddress = new DERObjectIdentifier("1.3.6.1.4.1.11591.2.1.1"); // pkaAddress
    public static final DERObjectIdentifier GnuRadar = new DERObjectIdentifier("1.3.6.1.4.1.11591.3"); // GNU Radar
    public static final DERObjectIdentifier digestAlgorithm = new DERObjectIdentifier("1.3.6.1.4.1.11591.12"); // digestAlgorithm
    public static final DERObjectIdentifier Tiger_192 = new DERObjectIdentifier("1.3.6.1.4.1.11591.12.2"); // TIGER/192
    public static final DERObjectIdentifier encryptionAlgorithm = new DERObjectIdentifier("1.3.6.1.4.1.11591.13"); // encryptionAlgorithm
    public static final DERObjectIdentifier Serpent = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2"); // Serpent
    public static final DERObjectIdentifier Serpent_128_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.1"); // Serpent-128-ECB
    public static final DERObjectIdentifier Serpent_128_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.2"); // Serpent-128-CBC
    public static final DERObjectIdentifier Serpent_128_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.3"); // Serpent-128-OFB
    public static final DERObjectIdentifier Serpent_128_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.4"); // Serpent-128-CFB
    public static final DERObjectIdentifier Serpent_192_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.21"); // Serpent-192-ECB
    public static final DERObjectIdentifier Serpent_192_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.22"); // Serpent-192-CBC
    public static final DERObjectIdentifier Serpent_192_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.23"); // Serpent-192-OFB
    public static final DERObjectIdentifier Serpent_192_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.24"); // Serpent-192-CFB
    public static final DERObjectIdentifier Serpent_256_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.41"); // Serpent-256-ECB
    public static final DERObjectIdentifier Serpent_256_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.42"); // Serpent-256-CBC
    public static final DERObjectIdentifier Serpent_256_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.43"); // Serpent-256-OFB
    public static final DERObjectIdentifier Serpent_256_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.44"); // Serpent-256-CFB
    public static final DERObjectIdentifier CRC = new DERObjectIdentifier("1.3.6.1.4.1.11591.14"); // CRC algorithms
    public static final DERObjectIdentifier CRC32 = new DERObjectIdentifier("1.3.6.1.4.1.11591.14.1"); // CRC 32
}
