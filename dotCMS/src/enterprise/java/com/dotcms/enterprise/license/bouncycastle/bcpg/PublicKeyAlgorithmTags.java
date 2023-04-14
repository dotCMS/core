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

package com.dotcms.enterprise.license.bouncycastle.bcpg;

/**
 * Public Key Algorithm tag numbers
 */
public interface PublicKeyAlgorithmTags 
{
    public static final int RSA_GENERAL = 1;       // RSA (Encrypt or Sign)
    public static final int RSA_ENCRYPT = 2;       // RSA Encrypt-Only
    public static final int RSA_SIGN = 3;          // RSA Sign-Only
    public static final int ELGAMAL_ENCRYPT = 16;  // Elgamal (Encrypt-Only), see [ELGAMAL]
    public static final int DSA = 17;              // DSA (Digital Signature Standard)
    public static final int EC = 18;               // Reserved for Elliptic Curve
    public static final int ECDSA = 19;            // Reserved for ECDSA
    public static final int ELGAMAL_GENERAL = 20;  // Elgamal (Encrypt or Sign)
    public static final int DIFFIE_HELLMAN = 21;   // Reserved for Diffie-Hellman (X9.42, as defined for IETF-S/MIME)

    public static final int EXPERIMENTAL_1 = 100;
    public static final int EXPERIMENTAL_2 = 101;
    public static final int EXPERIMENTAL_3 = 102;
    public static final int EXPERIMENTAL_4 = 103;
    public static final int EXPERIMENTAL_5 = 104;
    public static final int EXPERIMENTAL_6 = 105;
    public static final int EXPERIMENTAL_7 = 106;
    public static final int EXPERIMENTAL_8 = 107;
    public static final int EXPERIMENTAL_9 = 108;
    public static final int EXPERIMENTAL_10 = 109;
    public static final int EXPERIMENTAL_11 = 110;
}
