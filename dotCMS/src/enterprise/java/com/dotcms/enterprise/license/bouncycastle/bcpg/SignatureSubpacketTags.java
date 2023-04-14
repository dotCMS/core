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
 * Basic PGP signature sub-packet tag types.
 */
public interface SignatureSubpacketTags 
{
    public static final int CREATION_TIME = 2;         // signature creation time
    public static final int EXPIRE_TIME = 3;           // signature expiration time
    public static final int EXPORTABLE = 4;            // exportable certification
    public static final int TRUST_SIG = 5;             // trust signature
    public static final int REG_EXP = 6;               // regular expression
    public static final int REVOCABLE = 7;             // revocable
    public static final int KEY_EXPIRE_TIME = 9;       // key expiration time
    public static final int PLACEHOLDER = 10;          // placeholder for backward compatibility
    public static final int PREFERRED_SYM_ALGS = 11;   // preferred symmetric algorithms
    public static final int REVOCATION_KEY = 12;       // revocation key
    public static final int ISSUER_KEY_ID = 16;        // issuer key ID
    public static final int NOTATION_DATA = 20;        // notation data
    public static final int PREFERRED_HASH_ALGS = 21;  // preferred hash algorithms
    public static final int PREFERRED_COMP_ALGS = 22;  // preferred compression algorithms
    public static final int KEY_SERVER_PREFS = 23;     // key server preferences
    public static final int PREFERRED_KEY_SERV = 24;   // preferred key server
    public static final int PRIMARY_USER_ID = 25;      // primary user id
    public static final int POLICY_URL = 26;           // policy URL
    public static final int KEY_FLAGS = 27;            // key flags
    public static final int SIGNER_USER_ID = 28;       // signer's user id
    public static final int REVOCATION_REASON = 29;    // reason for revocation
    public static final int FEATURES = 30;             // features
    public static final int SIGNATURE_TARGET = 31;     // signature target
    public static final int EMBEDDED_SIGNATURE = 32;   // embedded signature
}
