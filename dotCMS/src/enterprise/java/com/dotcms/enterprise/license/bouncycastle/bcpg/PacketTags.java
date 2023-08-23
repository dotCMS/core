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
 * Basic PGP packet tag types.
 */
public interface PacketTags 
{
      public static final int RESERVED =  0 ;                //  Reserved - a packet tag must not have this value
      public static final int PUBLIC_KEY_ENC_SESSION = 1;    // Public-Key Encrypted Session Key Packet
      public static final int SIGNATURE = 2;                 // Signature Packet
      public static final int SYMMETRIC_KEY_ENC_SESSION = 3; // Symmetric-Key Encrypted Session Key Packet
      public static final int ONE_PASS_SIGNATURE = 4 ;       // One-Pass Signature Packet
      public static final int SECRET_KEY = 5;                // Secret Key Packet
      public static final int PUBLIC_KEY = 6 ;               // Public Key Packet
      public static final int SECRET_SUBKEY = 7;             // Secret Subkey Packet
      public static final int COMPRESSED_DATA = 8;           // Compressed Data Packet
      public static final int SYMMETRIC_KEY_ENC = 9;         // Symmetrically Encrypted Data Packet
      public static final int MARKER = 10;                   // Marker Packet
      public static final int LITERAL_DATA = 11;             // Literal Data Packet
      public static final int TRUST = 12;                    // Trust Packet
      public static final int USER_ID = 13;                  // User ID Packet
      public static final int PUBLIC_SUBKEY = 14;            // Public Subkey Packet
      public static final int USER_ATTRIBUTE = 17;           // User attribute
      public static final int SYM_ENC_INTEGRITY_PRO = 18;    // Symmetric encrypted, integrity protected
      public static final int MOD_DETECTION_CODE = 19;       // Modification detection code
      
      public static final int EXPERIMENTAL_1 = 60;           // Private or Experimental Values
      public static final int EXPERIMENTAL_2 = 61;
      public static final int EXPERIMENTAL_3 = 62;
      public static final int EXPERIMENTAL_4 = 63;
}
